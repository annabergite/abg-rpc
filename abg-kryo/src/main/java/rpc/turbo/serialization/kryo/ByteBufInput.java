package rpc.abg.serialization.kryo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import rpc.abg.serialization.SerializationConstants;

public class ByteBufInput extends Input {
	protected ByteBuf byteBuf;

	/** Creates a new Input for reading from a ByteBuf. */
	public ByteBufInput(ByteBuf byteBuf) {
		setBuffer(byteBuf);
	}

	/**
	 * Sets a new byteBuf, discarding any previous byteBuf. The byte order,
	 * position, limit and capacity are set to match the specified byteBuf. The
	 * total is reset. The {@link #setInputStream(InputStream) InputStream} is set
	 * to null.
	 */
	public void setBuffer(ByteBuf byteBuf) {
		if (byteBuf == null) {
			return;
		}

		this.byteBuf = byteBuf;
		position = byteBuf.readerIndex();
		limit = byteBuf.capacity();
		capacity = byteBuf.capacity();
		total = 0;
		inputStream = null;
	}

	/**
	 * Releases a direct byteBuf. {@link #setBuffer(ByteBuf)} must be called before
	 * any write operations can be performed.
	 */
	public void release() {
		close();
		byteBuf.release();
		byteBuf = null;
	}

	public ByteBuf getByteBuf() {
		return byteBuf;
	}

	public void rewind() {
		super.rewind();
		byteBuf.readerIndex(0);
	}

	/**
	 * Fills the byteBuf with more bytes. Can be overridden to fill the bytes from a
	 * source other than the InputStream.
	 */
	protected int fill(ByteBuf byteBuf, int offset, int count) throws KryoException {
		return -1;
	}

	private void ensureSize(int length) {
		if (buffer != null && buffer.length >= length) {
			return;
		}

		buffer = new byte[alignSize(length)];
	}

	private static final int alignSize(int cap) {
		int n = cap - 1;

		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;

		return (n < 0) ? 1 : n + 1;
	}

	/**
	 * Reads a single byte as an int from 0 to 255, or -1 if there are no more bytes
	 * are available.
	 */
	public int read() throws KryoException {
		return byteBuf.readByte() & 0xFF;
	}

	/**
	 * Reads bytes.length bytes or less and writes them to the specified byte[],
	 * starting at 0, and returns the number of bytes read.
	 */
	public int read(byte[] bytes) throws KryoException {
		return read(bytes, 0, bytes.length);
	}

	/**
	 * Reads count bytes or less and writes them to the specified byte[], starting
	 * at offset, and returns the number of bytes read or -1 if no more bytes are
	 * available.
	 */
	public int read(byte[] bytes, int offset, int count) throws KryoException {
		int index = byteBuf.writerIndex();
		byteBuf.readBytes(bytes, offset, count);
		int readCount = byteBuf.writerIndex() - index;
		return readCount == 0 ? -1 : readCount;
	}

	public void setPosition(int position) {
		this.position = position;
		this.byteBuf.readerIndex(position);
	}

	/** Sets the limit in the byteBuf. */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void skip(int count) throws KryoException {
		byteBuf.skipBytes(count);
	}

	/** Discards the specified number of bytes. */
	public long skip(long count) throws KryoException {
		byteBuf.skipBytes((int) count);
		return count;
	}

	/** Closes the underlying InputStream, if any. */
	public void close() throws KryoException {
	}

	/** Closes the underlying InputStream, if any. */
	public void clear() throws KryoException {
		byteBuf = null;
	}

	// byte

	/** Reads a single byte. */
	public byte readByte() throws KryoException {
		return byteBuf.readByte();
	}

	/** Reads a byte as an int from 0 to 255. */
	public int readByteUnsigned() throws KryoException {
		return byteBuf.readByte() & 0xFF;
	}

	/** Reads the specified number of bytes into a new byte[]. */
	public byte[] readBytes(int length) throws KryoException {
		byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return bytes;
	}

	/**
	 * Reads bytes.length bytes and writes them to the specified byte[], starting at
	 * index 0.
	 */
	public void readBytes(byte[] bytes) throws KryoException {
		readBytes(bytes, 0, bytes.length);
	}

	/**
	 * Reads count bytes and writes them to the specified byte[], starting at
	 * offset.
	 */
	public void readBytes(byte[] bytes, int offset, int count) throws KryoException {
		byteBuf.readBytes(bytes, offset, count);
	}

	public int readInt() throws KryoException {
		return byteBuf.readInt();
	}

	public int readInt(boolean optimizePositive) throws KryoException {
		return readVarInt(optimizePositive);
	}

	public int readVarInt(boolean optimizePositive) throws KryoException {
		int b = byteBuf.readByte();
		int result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = byteBuf.readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuf.readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuf.readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuf.readByte();
						result |= (b & 0x7F) << 28;
					}
				}
			}
		}
		return optimizePositive ? result : ((result >>> 1) ^ -(result & 1));
	}

	public String readStringFast() {
		byte typ = readByte();

		switch (typ) {
		case SerializationConstants.STRING_NULL:
			return null;

		case SerializationConstants.STRING_EMPTY:
			return "";

		case SerializationConstants.STRING_LATIN1: {
			int count = readVarInt(true);

			ensureSize(count);
			byte[] bytes = this.buffer;
			readBytes(bytes, 0, count);

			return new String(bytes, 0, count, StandardCharsets.ISO_8859_1);
		}

		case SerializationConstants.STRING_UTF8: {
			int count = readVarInt(true);

			ensureSize(count);
			byte[] bytes = this.buffer;
			readBytes(bytes, 0, count);

			return new String(bytes, 0, count, StandardCharsets.UTF_8);
		}

		default:
			break;
		}

		return null;
	}

	/**
	 * Reads the length and string of UTF8 characters, or null. This can read
	 * strings written by {@link Output#writeString(String)} ,
	 * {@link Output#writeString(CharSequence)}, and
	 * {@link Output#writeAscii(String)}.
	 * 
	 * @return May be null.
	 */
	public String readString() {
		return readStringFast();
	}

	/**
	 * Reads the length and string of UTF8 characters, or null. This can read
	 * strings written by {@link Output#writeString(String)} ,
	 * {@link Output#writeString(CharSequence)}, and
	 * {@link Output#writeAscii(String)}.
	 * 
	 * @return May be null.
	 */
	public StringBuilder readStringBuilder() {
		String str = readStringFast();
		StringBuilder builder = new StringBuilder(str);
		return builder;
	}

	/** Reads a 4 byte float. */
	public float readFloat() throws KryoException {
		return byteBuf.readFloat();
	}

	/** Reads a 1-5 byte float with reduced precision. */
	public float readFloat(float precision, boolean optimizePositive) throws KryoException {
		return readInt(optimizePositive) / (float) precision;
	}

	/** Reads a 2 byte short. */
	public short readShort() throws KryoException {
		return byteBuf.readShort();
	}

	/** Reads a 2 byte short as an int from 0 to 65535. */
	public int readShortUnsigned() throws KryoException {
		return byteBuf.readShort() & 0xFFFF;
	}

	/** Reads an 8 byte long. */
	public long readLong() throws KryoException {
		return byteBuf.readLong();
	}

	/** {@inheritDoc} */
	public long readLong(boolean optimizePositive) throws KryoException {
		return readVarLong(optimizePositive);
	}

	/** {@inheritDoc} */
	public long readVarLong(boolean optimizePositive) throws KryoException {
		int b = byteBuf.readByte();
		long result = b & 0x7F;
		if ((b & 0x80) != 0) {
			b = byteBuf.readByte();
			result |= (b & 0x7F) << 7;
			if ((b & 0x80) != 0) {
				b = byteBuf.readByte();
				result |= (b & 0x7F) << 14;
				if ((b & 0x80) != 0) {
					b = byteBuf.readByte();
					result |= (b & 0x7F) << 21;
					if ((b & 0x80) != 0) {
						b = byteBuf.readByte();
						result |= (long) (b & 0x7F) << 28;
						if ((b & 0x80) != 0) {
							b = byteBuf.readByte();
							result |= (long) (b & 0x7F) << 35;
							if ((b & 0x80) != 0) {
								b = byteBuf.readByte();
								result |= (long) (b & 0x7F) << 42;
								if ((b & 0x80) != 0) {
									b = byteBuf.readByte();
									result |= (long) (b & 0x7F) << 49;
									if ((b & 0x80) != 0) {
										b = byteBuf.readByte();
										result |= (long) b << 56;
									}
								}
							}
						}
					}
				}
			}
		}

		if (!optimizePositive)
			result = (result >>> 1) ^ -(result & 1);

		return result;
	}

	/** Reads a 1 byte boolean. */
	public boolean readBoolean() throws KryoException {
		return byteBuf.readByte() == 1 ? true : false;
	}

	/** Reads a 2 byte char. */
	public char readChar() throws KryoException {
		return byteBuf.readChar();
	}

	/** Reads an 8 bytes double. */
	public double readDouble() throws KryoException {
		return byteBuf.readDouble();
	}

	/** Reads a 1-9 byte double with reduced precision. */
	public double readDouble(double precision, boolean optimizePositive) throws KryoException {
		return readLong(optimizePositive) / (double) precision;
	}
}
