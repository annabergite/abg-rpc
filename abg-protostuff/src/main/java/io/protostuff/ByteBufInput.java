package io.protostuff;

import static io.protostuff.ProtobufException.invalidEndTag;
import static io.protostuff.ProtobufException.invalidTag;
import static io.protostuff.ProtobufException.invalidWireType;
import static io.protostuff.ProtobufException.misreportedSize;
import static io.protostuff.ProtobufException.negativeSize;
import static io.protostuff.WireFormat.TAG_TYPE_BITS;
import static io.protostuff.WireFormat.TAG_TYPE_MASK;
import static io.protostuff.WireFormat.WIRETYPE_END_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_FIXED32;
import static io.protostuff.WireFormat.WIRETYPE_FIXED64;
import static io.protostuff.WireFormat.WIRETYPE_LENGTH_DELIMITED;
import static io.protostuff.WireFormat.WIRETYPE_START_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_TAIL_DELIMITER;
import static io.protostuff.WireFormat.WIRETYPE_VARINT;
import static io.protostuff.WireFormat.getTagFieldNumber;
import static io.protostuff.WireFormat.getTagWireType;
import static io.protostuff.WireFormat.makeTag;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.protostuff.StringSerializer.STRING;
import rpc.abg.serialization.SerializationConstants;
import rpc.abg.util.ByteBufUtils;
import rpc.abg.util.UnsafeStringUtils;
import rpc.abg.util.concurrent.ThreadLocalBytes;

public final class ByteBufInput implements Input {

	private ByteBuf byteBuf;
	private int lastTag = 0;
	private int packedLimit = 0;

	/**
	 * If true, the nested messages are group-encoded
	 */
	private boolean decodeNestedMessageAsGroup;

	/**
	 * An input for a ByteBuffer
	 * 
	 * @param byteBuf
	 *            the buffer to read from, it will be sliced
	 * @param protostuffMessage
	 *            if we are parsing a protostuff (true) or protobuf (false) message
	 */
	public ByteBufInput(ByteBuf byteBuf, boolean protostuffMessage) {
		this.byteBuf = byteBuf;
		this.decodeNestedMessageAsGroup = protostuffMessage;
	}

	public ByteBuf getByteBuf() {
		return byteBuf;
	}

	public void setByteBuf(ByteBuf buffer, boolean protostuffMessage) {
		this.byteBuf = buffer;
		this.decodeNestedMessageAsGroup = protostuffMessage;
		this.lastTag = 0;
		this.packedLimit = 0;
	}

	/**
	 * Resets the offset and the limit of the internal buffer.
	 */
	public ByteBufInput reset(int offset, int len) {
		return this;
	}

	/**
	 * Returns the current offset (the position).
	 */
	public int currentOffset() {
		return byteBuf.readerIndex();
	}

	/**
	 * Returns the current limit (the end index).
	 */
	public int currentLimit() {
		return byteBuf.writerIndex();
	}

	/**
	 * Return true if currently reading packed field
	 */
	public boolean isCurrentFieldPacked() {
		return packedLimit != 0 && packedLimit != byteBuf.readerIndex();
	}

	/**
	 * Returns the last tag.
	 */
	public int getLastTag() {
		return lastTag;
	}

	/**
	 * Attempt to read a field tag, returning zero if we have reached EOF. Protocol
	 * message parsers use this to read tags, since a protocol message may legally
	 * end wherever a tag occurs, and zero is not a valid tag number.
	 */
	public int readTag() throws IOException {
		if (byteBuf.readableBytes() == 0) {
			lastTag = 0;
			return 0;
		}

		final int tag = readRawVarint32();
		if (tag >>> TAG_TYPE_BITS == 0) {
			// If we actually read zero, that's not a valid tag.
			throw invalidTag();
		}
		lastTag = tag;
		return tag;
	}

	/**
	 * Verifies that the last call to readTag() returned the given tag value. This
	 * is used to verify that a nested group ended with the correct end tag.
	 * 
	 * @throws ProtobufException
	 *             {@code value} does not match the last tag.
	 */
	public void checkLastTagWas(final int value) throws ProtobufException {
		if (lastTag != value) {
			throw invalidEndTag();
		}
	}

	/**
	 * Reads and discards a single field, given its tag value.
	 * 
	 * @return {@code false} if the tag is an endgroup tag, in which case nothing is
	 *         skipped. Otherwise, returns {@code true}.
	 */
	public boolean skipField(final int tag) throws IOException {
		switch (getTagWireType(tag)) {
		case WIRETYPE_VARINT:
			readInt32();
			return true;
		case WIRETYPE_FIXED64:
			readRawLittleEndian64();
			return true;
		case WIRETYPE_LENGTH_DELIMITED:
			final int size = readRawVarint32();
			if (size < 0)
				throw negativeSize();
			byteBuf.readerIndex(byteBuf.readerIndex() + size);
			// offset += size;
			return true;
		case WIRETYPE_START_GROUP:
			skipMessage();
			checkLastTagWas(makeTag(getTagFieldNumber(tag), WIRETYPE_END_GROUP));
			return true;
		case WIRETYPE_END_GROUP:
			return false;
		case WIRETYPE_FIXED32:
			readRawLittleEndian32();
			return true;
		default:
			throw invalidWireType();
		}
	}

	/**
	 * Reads and discards an entire message. This will read either until EOF or
	 * until an endgroup tag, whichever comes first.
	 */
	public void skipMessage() throws IOException {
		while (true) {
			final int tag = readTag();
			if (tag == 0 || !skipField(tag)) {
				return;
			}
		}
	}

	@Override
	public <T> void handleUnknownField(int fieldNumber, Schema<T> schema) throws IOException {
		skipField(lastTag);
	}

	@Override
	public <T> int readFieldNumber(Schema<T> schema) throws IOException {
		if (byteBuf.readableBytes() == 0) {
			lastTag = 0;
			return 0;
		}

		// are we reading packed field?
		if (isCurrentFieldPacked()) {
			if (packedLimit < byteBuf.readerIndex())
				throw misreportedSize();

			// Return field number while reading packed field
			return lastTag >>> TAG_TYPE_BITS;
		}

		packedLimit = 0;
		final int tag = readRawVarint32();
		final int fieldNumber = tag >>> TAG_TYPE_BITS;
		if (fieldNumber == 0) {
			if (decodeNestedMessageAsGroup && WIRETYPE_TAIL_DELIMITER == (tag & TAG_TYPE_MASK)) {
				// protostuff's tail delimiter for streaming
				// 2 options: length-delimited or tail-delimited.
				lastTag = 0;
				return 0;
			}
			// If we actually read zero, that's not a valid tag.
			throw invalidTag();
		}
		if (decodeNestedMessageAsGroup && WIRETYPE_END_GROUP == (tag & TAG_TYPE_MASK)) {
			lastTag = 0;
			return 0;
		}

		lastTag = tag;
		return fieldNumber;
	}

	/**
	 * Check if this field have been packed into a length-delimited field. If so,
	 * update internal state to reflect that packed fields are being read.
	 * 
	 * @throws IOException
	 */
	private void checkIfPackedField() throws IOException {
		// Do we have the start of a packed field?
		if (packedLimit == 0 && getTagWireType(lastTag) == WIRETYPE_LENGTH_DELIMITED) {
			final int length = readRawVarint32();
			if (length < 0)
				throw negativeSize();

			if (length > byteBuf.readableBytes())
				throw misreportedSize();

			this.packedLimit = byteBuf.readerIndex() + length;
		}
	}

	/**
	 * Read a {@code double} field value from the internal buffer.
	 */
	@Override
	public double readDouble() throws IOException {
		checkIfPackedField();
		return Double.longBitsToDouble(readRawLittleEndian64());
	}

	/**
	 * Read a {@code float} field value from the internal buffer.
	 */
	@Override
	public float readFloat() throws IOException {
		checkIfPackedField();
		return Float.intBitsToFloat(readRawLittleEndian32());
	}

	/**
	 * Read a {@code uint64} field value from the internal buffer.
	 */
	@Override
	public long readUInt64() throws IOException {
		checkIfPackedField();
		return readRawVarint64();
	}

	/**
	 * Read an {@code int64} field value from the internal buffer.
	 */
	@Override
	public long readInt64() throws IOException {
		checkIfPackedField();
		return readRawVarint64();
	}

	/**
	 * Read an {@code int32} field value from the internal buffer.
	 */
	@Override
	public int readInt32() throws IOException {
		checkIfPackedField();
		return readRawVarint32();
	}

	/**
	 * Read a {@code fixed64} field value from the internal buffer.
	 */
	@Override
	public long readFixed64() throws IOException {
		checkIfPackedField();
		return readRawLittleEndian64();
	}

	/**
	 * Read a {@code fixed32} field value from the internal buffer.
	 */
	@Override
	public int readFixed32() throws IOException {
		checkIfPackedField();
		return readRawLittleEndian32();
	}

	/**
	 * Read a {@code bool} field value from the internal buffer.
	 */
	@Override
	public boolean readBool() throws IOException {
		checkIfPackedField();
		return byteBuf.readByte() != 0;
	}

	/**
	 * Read a {@code uint32} field value from the internal buffer.
	 */
	@Override
	public int readUInt32() throws IOException {
		checkIfPackedField();
		return readRawVarint32();
	}

	/**
	 * Read an enum field value from the internal buffer. Caller is responsible for
	 * converting the numeric value to an actual enum.
	 */
	@Override
	public int readEnum() throws IOException {
		checkIfPackedField();
		return readRawVarint32();
	}

	/**
	 * Read an {@code sfixed32} field value from the internal buffer.
	 */
	@Override
	public int readSFixed32() throws IOException {
		checkIfPackedField();
		return readRawLittleEndian32();
	}

	/**
	 * Read an {@code sfixed64} field value from the internal buffer.
	 */
	@Override
	public long readSFixed64() throws IOException {
		checkIfPackedField();
		return readRawLittleEndian64();
	}

	/**
	 * Read an {@code sint32} field value from the internal buffer.
	 */
	@Override
	public int readSInt32() throws IOException {
		checkIfPackedField();
		final int n = readRawVarint32();
		return (n >>> 1) ^ -(n & 1);
	}

	/**
	 * Read an {@code sint64} field value from the internal buffer.
	 */
	@Override
	public long readSInt64() throws IOException {
		checkIfPackedField();
		final long n = readRawVarint64();
		return (n >>> 1) ^ -(n & 1);
	}

	@Override
	public String readString() throws IOException {
		if (SerializationConstants.INCOMPATIBLE_FAST_STRING_FORMAT) {
			return readStringFast();
		}

		final int length = readRawVarint32();
		if (length < 0) {
			throw negativeSize();
		}

		if (byteBuf.readableBytes() < length) {
			throw misreportedSize();
		}

		byte[] bytes = ThreadLocalBytes.current();
		byteBuf.readBytes(bytes, 0, length);

		return STRING.deser(bytes, 0, length);
	}

	private String readStringFast() throws IOException {
		byte typ = byteBuf.readByte();

		switch (typ) {
		case SerializationConstants.STRING_NULL:
			return null;

		case SerializationConstants.STRING_EMPTY:
			return "";

		case SerializationConstants.STRING_LATIN1: {
			int length = readRawVarint32();

			if (length < 0) {
				throw negativeSize();
			}

			if (byteBuf.readableBytes() < length) {
				throw misreportedSize();
			}

			byte[] bytes = new byte[length];
			byteBuf.readBytes(bytes, 0, length);

			return UnsafeStringUtils.toLatin1String(bytes);
		}

		case SerializationConstants.STRING_UTF8: {
			int length = readRawVarint32();

			if (length < 0) {
				throw negativeSize();
			}

			if (byteBuf.readableBytes() < length) {
				throw misreportedSize();
			}

			byte[] bytes = ThreadLocalBytes.current();
			byteBuf.readBytes(bytes, 0, length);

			return STRING.deser(bytes, 0, length);
		}

		default:
			return null;
		}
	}

	@Override
	public ByteString readBytes() throws IOException {
		return ByteString.wrap(readByteArray());
	}

	@Override
	public void readBytes(final ByteBuffer bb) throws IOException {
		final int length = readRawVarint32();
		if (length < 0)
			throw negativeSize();

		if (byteBuf.readableBytes() < length)
			throw misreportedSize();

		byteBuf.readBytes(bb);
	}

	@Override
	public byte[] readByteArray() throws IOException {
		final int length = readRawVarint32();
		if (length < 0)
			throw negativeSize();

		if (byteBuf.readableBytes() < length)
			// if(offset + length > limit)
			throw misreportedSize();

		final byte[] copy = new byte[length];
		byteBuf.readBytes(copy);
		return copy;
	}

	@Override
	public <T> T mergeObject(T value, final Schema<T> schema) throws IOException {
		if (decodeNestedMessageAsGroup)
			return mergeObjectEncodedAsGroup(value, schema);

		final int length = readRawVarint32();
		if (length < 0)
			throw negativeSize();

		if (byteBuf.readableBytes() < length)
			throw misreportedSize();

		ByteBuf dup = byteBuf.slice();
		dup.readerIndex(length);

		// save old limit
		// final int oldLimit = this.limit;

		// this.limit = offset + length;

		if (value == null)
			value = schema.newMessage();
		ByteBufInput nestedInput = new ByteBufInput(dup, decodeNestedMessageAsGroup);
		schema.mergeFrom(nestedInput, value);
		if (!schema.isInitialized(value))
			throw new UninitializedMessageException(value, schema);
		nestedInput.checkLastTagWas(0);
		// checkLastTagWas(0);

		// restore old limit
		// this.limit = oldLimit;

		byteBuf.readerIndex(byteBuf.readerIndex() + length);
		return value;
	}

	private <T> T mergeObjectEncodedAsGroup(T value, final Schema<T> schema) throws IOException {
		if (value == null)
			value = schema.newMessage();
		schema.mergeFrom(this, value);
		if (!schema.isInitialized(value))
			throw new UninitializedMessageException(value, schema);
		// handling is in #readFieldNumber
		checkLastTagWas(0);
		return value;
	}

	/**
	 * Reads a var int 32 from the internal byte buffer.
	 */
	public int readRawVarint32() throws IOException {
		return ByteBufUtils.readVarInt(byteBuf);
	}

	/**
	 * Reads a var int 64 from the internal byte buffer.
	 */
	public long readRawVarint64() throws IOException {
		return ByteBufUtils.readVarLong(byteBuf);
	}

	/**
	 * Read a 32-bit little-endian integer from the internal buffer.
	 */
	public int readRawLittleEndian32() throws IOException {
		return byteBuf.readIntLE();
	}

	/**
	 * Read a 64-bit little-endian integer from the internal byte buffer.
	 */
	public long readRawLittleEndian64() throws IOException {
		return byteBuf.readLongLE();
	}

	@Override
	public void transferByteRangeTo(Output output, boolean utf8String, int fieldNumber, boolean repeated)
			throws IOException {
		final int length = readRawVarint32();
		if (length < 0)
			throw negativeSize();

		if (utf8String) {
			// if it is a UTF string, we have to call the writeByteRange.

			if (byteBuf.hasArray()) {
				output.writeByteRange(true, fieldNumber, byteBuf.array(), byteBuf.arrayOffset() + byteBuf.readerIndex(),
						length, repeated);
				byteBuf.readerIndex(byteBuf.readerIndex() + length);
			} else {
				byte[] bytes = new byte[length];
				byteBuf.readBytes(bytes);
				output.writeByteRange(true, fieldNumber, bytes, 0, bytes.length, repeated);
			}
		} else {
			// Do the potentially vastly more efficient potential splice call.
			if (byteBuf.readableBytes() < length)
				throw misreportedSize();

			ByteBuf dup = byteBuf.slice();
			dup.readerIndex(length);

			if (output instanceof ByteBufOutput) {
				((ByteBufOutput) output).writeBytes(fieldNumber, dup, repeated);
			} else {
				ByteBuffer dst = ByteBuffer.allocate(byteBuf.readableBytes());
				dup.readBytes(dst);
				output.writeBytes(fieldNumber, dst, repeated);
			}

			byteBuf.readerIndex(byteBuf.readerIndex() + length);
		}

		// output.writeByteRange(utf8String, fieldNumber, buffer, offset, length,
		// repeated);
		//
		// offset += length;
	}

	/**
	 * Reads a byte array/ByteBuffer value.
	 */
	@Override
	public ByteBuffer readByteBuffer() throws IOException {
		return ByteBuffer.wrap(readByteArray());
	}

}
