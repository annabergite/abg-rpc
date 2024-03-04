package io.protostuff;

import static io.protostuff.ProtobufOutput.encodeZigZag32;
import static io.protostuff.ProtobufOutput.encodeZigZag64;
import static io.protostuff.WireFormat.WIRETYPE_END_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_FIXED32;
import static io.protostuff.WireFormat.WIRETYPE_FIXED64;
import static io.protostuff.WireFormat.WIRETYPE_LENGTH_DELIMITED;
import static io.protostuff.WireFormat.WIRETYPE_START_GROUP;
import static io.protostuff.WireFormat.WIRETYPE_VARINT;
import static io.protostuff.WireFormat.makeTag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import rpc.abg.serialization.SerializationConstants;
import rpc.abg.util.ByteBufUtils;
import rpc.abg.util.UnsafeStringUtils;

public final class ByteBufOutput implements Output {
	private ByteBuf byteBuf;

	public ByteBufOutput(final ByteBuf buffer) {
		this.byteBuf = buffer;
	}

	public void setByteBuf(ByteBuf buffer) {
		this.byteBuf = buffer;
	}

	public ByteBuf getByteBuf() {
		return byteBuf;
	}

	public void writeRawInt32(int value) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, value);
	}

	@Override
	public void writeInt32(int fieldNumber, int value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarInt(byteBuf, value);
	}

	@Override
	public void writeUInt32(int fieldNumber, int value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarInt(byteBuf, value);
	}

	@Override
	public void writeSInt32(int fieldNumber, int value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarInt(byteBuf, encodeZigZag32(value));
	}

	@Override
	public void writeFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED32));
		byteBuf.writeIntLE(value);
	}

	@Override
	public void writeSFixed32(int fieldNumber, int value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED32));
		byteBuf.writeIntLE(value);
	}

	@Override
	public void writeInt64(int fieldNumber, long value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarLong(byteBuf, value);
	}

	@Override
	public void writeUInt64(int fieldNumber, long value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarLong(byteBuf, value);
	}

	@Override
	public void writeSInt64(int fieldNumber, long value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		ByteBufUtils.writeVarLong(byteBuf, encodeZigZag64(value));
	}

	@Override
	public void writeFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED64));
		byteBuf.writeLongLE(value);
	}

	@Override
	public void writeSFixed64(int fieldNumber, long value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED64));
		byteBuf.writeLongLE(value);
	}

	@Override
	public void writeFloat(int fieldNumber, float value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED32));
		byteBuf.writeLongLE(Float.floatToRawIntBits(value));
	}

	@Override
	public void writeDouble(int fieldNumber, double value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_FIXED64));
		byteBuf.writeLongLE(Double.doubleToRawLongBits(value));
	}

	@Override
	public void writeBool(int fieldNumber, boolean value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_VARINT));
		byteBuf.writeByte(value ? (byte) 0x01 : 0x00);
	}

	public void writeRawBool(boolean value) throws IOException {
		byteBuf.writeByte(value ? (byte) 0x01 : 0x00);
	}

	@Override
	public void writeEnum(int fieldNumber, int number, boolean repeated) throws IOException {
		writeInt32(fieldNumber, number, repeated);
	}

	public void writeRawString(String value) throws IOException {
		if (value == null) {
			byteBuf.writeByte(SerializationConstants.STRING_NULL);
			return;
		}

		if (value.length() == 0) {
			byteBuf.writeByte(SerializationConstants.STRING_EMPTY);
			return;
		}

		byte[] bytes;
		if (UnsafeStringUtils.isLatin1(value)) {
			byteBuf.writeByte(SerializationConstants.STRING_LATIN1);
			bytes = UnsafeStringUtils.getLatin1Bytes(value);
		} else {
			byteBuf.writeByte(SerializationConstants.STRING_UTF8);
			bytes = value.getBytes(StandardCharsets.UTF_8);
		}

		ByteBufUtils.writeVarInt(byteBuf, bytes.length);
		byteBuf.writeBytes(bytes, 0, bytes.length);
	}

	@Override
	public void writeString(int fieldNumber, CharSequence value, boolean repeated) throws IOException {
		if (SerializationConstants.INCOMPATIBLE_FAST_STRING_FORMAT) {
			writeStringFast(fieldNumber, value.toString());
			return;
		}

		writeByteArray(fieldNumber, UnsafeStringUtils.getUTF8Bytes(value.toString()), repeated);
	}

	private void writeStringFast(int fieldNumber, String value) {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));

		if (value == null) {
			byteBuf.writeByte(SerializationConstants.STRING_NULL);
			return;
		}

		if (value.length() == 0) {
			byteBuf.writeByte(SerializationConstants.STRING_EMPTY);
			return;
		}

		byte[] bytes;
		if (UnsafeStringUtils.isLatin1(value)) {
			byteBuf.writeByte(SerializationConstants.STRING_LATIN1);
			bytes = UnsafeStringUtils.getLatin1Bytes(value);
		} else {
			byteBuf.writeByte(SerializationConstants.STRING_UTF8);
			bytes = value.getBytes(StandardCharsets.UTF_8);
		}

		ByteBufUtils.writeVarInt(byteBuf, bytes.length);
		byteBuf.writeBytes(bytes, 0, bytes.length);
	}

	@Override
	public void writeBytes(int fieldNumber, ByteString value, boolean repeated) throws IOException {
		writeByteArray(fieldNumber, value.getBytes(), repeated);
	}

	@Override
	public void writeByteArray(int fieldNumber, byte[] bytes, boolean repeated) throws IOException {
		writeByteRange(false, fieldNumber, bytes, 0, bytes.length, repeated);
	}

	@Override
	public void writeByteRange(boolean utf8String, int fieldNumber, byte[] value, int offset, int length,
			boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));
		ByteBufUtils.writeVarInt(byteBuf, length);
		byteBuf.writeBytes(value, offset, length);
	}

	@Override
	public <T> void writeObject(final int fieldNumber, final T value, final Schema<T> schema, final boolean repeated)
			throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_START_GROUP));
		schema.writeTo(this, value);
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_END_GROUP));
	}

	@Override
	public void writeBytes(int fieldNumber, ByteBuffer value, boolean repeated) throws IOException {
		writeByteRange(false, fieldNumber, value.array(), value.arrayOffset() + value.position(), value.remaining(),
				repeated);
	}

	public void writeBytes(int fieldNumber, ByteBuf value, boolean repeated) throws IOException {
		ByteBufUtils.writeVarInt(byteBuf, makeTag(fieldNumber, WIRETYPE_LENGTH_DELIMITED));
		ByteBufUtils.writeVarInt(byteBuf, value.readableBytes());

		byteBuf.writeBytes(value);
	}

}
