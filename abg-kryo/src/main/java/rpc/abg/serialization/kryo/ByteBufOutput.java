package rpc.abg.serialization.kryo;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import rpc.abg.serialization.SerializationConstants;
import rpc.abg.util.UnsafeStringUtils;

public class ByteBufOutput extends Output {
	private static final int MAX_SAFE_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	protected ByteBuf byteBuf;

	/** Creates a new Output for writing to a ByteBuf. */
	public ByteBufOutput(ByteBuf buffer) {
		setBuffer(buffer);
	}

	/**
	 * Release a direct buffer. {@link #setBuffer(ByteBuf, int)} should be called
	 * before next write operations can be called.
	 * 
	 * NOTE: If Cleaner is not accessible due to SecurityManager restrictions,
	 * reflection could be used to obtain the "clean" method and then invoke it.
	 */
	public void release() {
		clear();
		byteBuf.release();
		byteBuf = null;
	}

	/**
	 * Sets the buffer that will be written to. maxCapacity is set to the specified
	 * buffer's capacity.
	 * 
	 * @see #setBuffer(ByteBuf, int)
	 */
	public void setBuffer(ByteBuf buffer) {
		if (buffer == null) {
			return;
		}

		setBuffer(buffer, buffer.capacity());
	}

	/**
	 * Sets the buffer that will be written to. The byte order, position and
	 * capacity are set to match the specified buffer. The total is set to 0. The
	 * {@link #setOutputStream(OutputStream) OutputStream} is set to null.
	 * 
	 * @param maxBufferSize
	 *            The buffer is doubled as needed until it exceeds maxCapacity and
	 *            an exception is thrown.
	 */
	public void setBuffer(ByteBuf buffer, int maxBufferSize) {
		if (buffer == null) {
			return;
		}

		if (maxBufferSize < -1) {
			throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
		}

		this.byteBuf = buffer;
		this.maxCapacity = maxBufferSize == -1 ? MAX_SAFE_ARRAY_SIZE : maxBufferSize;
		capacity = buffer.capacity();
		position = buffer.writerIndex();
		total = 0;
		outputStream = null;
	}

	/**
	 * Returns the buffer. The bytes between zero and {@link #position()} are the
	 * data that has been written.
	 */
	public ByteBuf getByteBuffer() {
		byteBuf.writerIndex(position);
		return byteBuf;
	}

	/**
	 * Returns a new byte array containing the bytes currently in the buffer between
	 * zero and {@link #position()}.
	 */
	public byte[] toBytes() {
		byte[] newBuffer = new byte[position];
		byteBuf.writerIndex(0);
		byteBuf.readBytes(newBuffer, 0, position);
		return newBuffer;
	}

	/** Sets the current position in the buffer. */
	public void setPosition(int position) {
		this.position = position;
		this.byteBuf.writerIndex(position);
	}

	/** Sets the position and total to zero. */
	public void clear() {
		byteBuf = null;
		position = 0;
		total = 0;
	}

	// OutputStream

	/** Writes the buffered bytes to the underlying OutputStream, if any. */
	public void flush() throws KryoException {
		total += position;
		position = 0;
	}

	/**
	 * Flushes any buffered bytes and closes the underlying OutputStream, if any.
	 */
	public void close() throws KryoException {
		flush();

		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException ignored) {
			}
		}
	}

	/** Writes a byte. */
	public void write(int value) throws KryoException {
		byteBuf.writeByte((byte) value);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write(byte[] bytes) throws KryoException {
		if (bytes == null) {
			throw new IllegalArgumentException("bytes cannot be null.");
		}

		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void write(byte[] bytes, int offset, int length) throws KryoException {
		writeBytes(bytes, offset, length);
	}

	// byte

	public void writeByte(byte value) throws KryoException {
		byteBuf.writeByte(value);
	}

	public void writeByte(int value) throws KryoException {
		byteBuf.writeByte((byte) value);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes(byte[] bytes) throws KryoException {
		if (bytes == null) {
			throw new IllegalArgumentException("bytes cannot be null.");
		}

		writeBytes(bytes, 0, bytes.length);
	}

	/** Writes the bytes. Note the byte[] length is not written. */
	public void writeBytes(byte[] bytes, int offset, int count) throws KryoException {
		byteBuf.writeBytes(bytes, offset, count);
	}

	// int

	/** Writes a 4 byte int. */
	public void writeInt(int value) throws KryoException {
		byteBuf.writeInt(value);
	}

	public int writeInt(int value, boolean optimizePositive) throws KryoException {
		return writeIntS(value, optimizePositive);
	}

	public int writeVarInt(int val, boolean optimizePositive) throws KryoException {
		return writeIntS(val, optimizePositive);
	}

	private void writeStringFast(String value) {
		if (value == null) {
			writeByte(SerializationConstants.STRING_NULL);
			return;
		}

		int length = value.length();
		if (length == 0) {
			writeByte(SerializationConstants.STRING_EMPTY);
			return;
		}

		if (UnsafeStringUtils.isLatin1(value)) {
			writeByte(SerializationConstants.STRING_LATIN1);
			byte[] bytes = UnsafeStringUtils.getLatin1Bytes(value);

			writeVarInt(length, true);
			byteBuf.writeBytes(bytes, 0, length);
		} else {
			writeByte(SerializationConstants.STRING_UTF8);
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

			writeVarInt(bytes.length, true);
			byteBuf.writeBytes(bytes);
		}

	}

	// string

	/**
	 * Writes the length and string, or null. Short strings are checked and if ASCII
	 * they are written more efficiently, else they are written as UTF8. If a string
	 * is known to be ASCII, {@link #writeAscii(String)} may be used. The string can
	 * be read using {@link Input#readString()} or
	 * {@link Input#readStringBuilder()}.
	 * 
	 * @param value
	 *            May be null.
	 */
	public void writeString(String value) throws KryoException {
		writeStringFast(value);
	}

	/**
	 * Writes the length and CharSequence as UTF8, or null. The string can be read
	 * using {@link Input#readString()} or {@link Input#readStringBuilder()}.
	 * 
	 * @param value
	 *            May be null.
	 */
	public void writeString(CharSequence value) throws KryoException {
		if (value == null) {
			writeStringFast(null);
			return;
		}

		writeStringFast(value.toString());
		return;
	}

	// float

	/** Writes a 4 byte float. */
	public void writeFloat(float value) throws KryoException {
		byteBuf.writeFloat(value);
	}

	/**
	 * Writes a 1-5 byte float with reduced precision.
	 * 
	 * @param optimizePositive
	 *            If true, small positive numbers will be more efficient (1 byte)
	 *            and small negative numbers will be inefficient (5 bytes).
	 */
	public int writeFloat(float value, float precision, boolean optimizePositive) throws KryoException {
		return writeInt((int) (value * precision), optimizePositive);
	}

	// short

	/** Writes a 2 byte short. */
	public void writeShort(int value) throws KryoException {
		byteBuf.writeShort((short) value);
	}

	// long

	/** Writes an 8 byte long. */
	public void writeLong(long value) throws KryoException {
		byteBuf.writeLong(value);
	}

	public int writeLong(long value, boolean optimizePositive) throws KryoException {
		return writeLongS(value, optimizePositive);
	}

	public int writeVarLong(long value, boolean optimizePositive) throws KryoException {
		return writeLongS(value, optimizePositive);
	}

	/**
	 * Writes a 1-9 byte long.
	 * 
	 * @param optimizePositive
	 *            If true, small positive numbers will be more efficient (1 byte)
	 *            and small negative numbers will be inefficient (9 bytes).
	 */
	public int writeLongS(long value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive)
			value = (value << 1) ^ (value >> 63);
		if (value >>> 7 == 0) {
			byteBuf.writeByte((byte) value);
			return 1;
		}
		if (value >>> 14 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7);
			byteBuf.writeShort(newValue);
			return 2;
		}
		if (value >>> 21 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 8) | (intValue >>> 7 & 0xFF | 0x80);
			byteBuf.writeShort(newValue);
			byteBuf.writeByte((byte) (intValue >>> 14));
			return 3;
		}
		if (value >>> 28 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21);

			byteBuf.writeInt(newValue);
			return 4;
		}
		if (value >>> 35 == 0) {
			int intValue = (int) value;
			int newValue = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(newValue);
			byteBuf.writeByte((byte) (value >>> 28));
			return 5;
		}
		if (value >>> 42 == 0) {
			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8) //
					| (value >>> 35));

			byteBuf.writeShort(second);
			return 6;
		}
		if (value >>> 49 == 0) {
			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			int second = (int) (((value >>> 28 & 0xFF | 0x80) << 8) //
					| (value >>> 35 & 0xFF | 0x80));

			byteBuf.writeShort(second);
			byteBuf.writeByte((byte) (value >>> 42));
			return 7;
		}
		if (value >>> 56 == 0) {
			int intValue = (int) value;
			int first = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21 & 0xFF | 0x80);

			byteBuf.writeInt(first);

			intValue = (int) (value >>> 28);
			int second = (((intValue & 0x7F) | 0x80) << 24) //
					| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
					| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
					| (intValue >>> 21);

			byteBuf.writeInt(second);
			return 8;
		}

		int intValue = (int) value;
		int first = (((intValue & 0x7F) | 0x80) << 24) //
				| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
				| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
				| (intValue >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(first);

		intValue = (int) (value >>> 28);
		int second = (((intValue & 0x7F) | 0x80) << 24) //
				| ((intValue >>> 7 & 0xFF | 0x80) << 16) //
				| ((intValue >>> 14 & 0xFF | 0x80) << 8) //
				| (intValue >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(second);
		byteBuf.writeByte((byte) (value >>> 56));
		return 9;
	}

	public int writeIntS(int value, boolean optimizePositive) throws KryoException {
		if (!optimizePositive)
			value = (value << 1) ^ (value >> 31);

		if (value >>> 7 == 0) {
			byteBuf.writeByte((byte) value);
			return 1;
		}

		if (value >>> 14 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7);
			byteBuf.writeShort(newValue);

			return 2;
		}

		if (value >>> 21 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 8) | (value >>> 7 & 0xFF | 0x80);
			byteBuf.writeShort(newValue);
			byteBuf.writeByte((byte) (value >>> 14));

			return 3;
		}

		if (value >>> 28 == 0) {
			int newValue = (((value & 0x7F) | 0x80) << 24) //
					| ((value >>> 7 & 0xFF | 0x80) << 16) //
					| ((value >>> 14 & 0xFF | 0x80) << 8) //
					| (value >>> 21);

			byteBuf.writeInt(newValue);

			return 4;
		}

		int newValue = (((value & 0x7F) | 0x80) << 24) //
				| ((value >>> 7 & 0xFF | 0x80) << 16) //
				| ((value >>> 14 & 0xFF | 0x80) << 8) //
				| (value >>> 21 & 0xFF | 0x80);

		byteBuf.writeInt(newValue);
		byteBuf.writeByte((byte) (value >>> 28));

		return 5;
	}

	// boolean

	/** Writes a 1 byte boolean. */
	public void writeBoolean(boolean value) throws KryoException {
		byteBuf.writeByte((byte) (value ? 1 : 0));
	}

	// char

	/** Writes a 2 byte char. */
	public void writeChar(char value) throws KryoException {
		byteBuf.writeChar(value);
	}

	// double

	/** Writes an 8 byte double. */
	public void writeDouble(double value) throws KryoException {
		byteBuf.writeDouble(value);
	}

	/**
	 * Writes a 1-9 byte double with reduced precision.
	 * 
	 * @param optimizePositive
	 *            If true, small positive numbers will be more efficient (1 byte)
	 *            and small negative numbers will be inefficient (9 bytes).
	 */
	public int writeDouble(double value, double precision, boolean optimizePositive) throws KryoException {
		return writeLong((long) (value * precision), optimizePositive);
	}

}
