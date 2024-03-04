package rpc.abg.util.uuid;

import static java.time.ZoneOffset.UTC;
import static rpc.abg.util.HexUtils.byteToHexLE;
import static rpc.abg.util.UnsafeUtils.unsafe;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.ThreadLocalRandom;
import rpc.abg.util.SystemClock;
import rpc.abg.util.UnsafeStringUtils;

/**
 * <p>
 * A globally unique identifier for objects.
 * </p>
 *
 * <p>
 * Consists of 12 bytes, divided as follows:
 * </p>
 * <table border="1">
 * <caption>RandomId layout</caption>
 * <tr>
 * <td>0</td>
 * <td>1</td>
 * <td>2</td>
 * <td>3</td>
 * <td>4</td>
 * <td>5</td>
 * <td>6</td>
 * <td>7</td>
 * <td>8</td>
 * <td>9</td>
 * <td>10</td>
 * <td>11</td>
 * </tr>
 * <tr>
 * <td colspan="4">time</td>
 * <td colspan="8">random long</td>
 * </tr>
 * </table>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 *
 */
public final class RandomId implements Serializable {

	private static final long serialVersionUID = -2951994548246899462L;

	private static final long HEX32_PADDING = ByteBuffer.wrap("0000000000000000".getBytes()).getLong();
	public final static long OFFSET_SECONDS = LocalDateTime.parse("2018-01-01T00:00:00").toEpochSecond(UTC);

	public final int timestamp;
	public final long random;

	/**
	 * Gets a new object id.
	 *
	 * @return the new id
	 */
	public static RandomId next() {
		return new RandomId(currentSeconds(), ThreadLocalRandom.current().nextLong());
	}

	public RandomId(final int timestamp, final long random) {
		this.timestamp = timestamp;
		this.random = random;
	}

	public RandomId(final ByteBuf buffer) {
		Objects.requireNonNull(buffer, "buffer");

		timestamp = buffer.readInt();
		random = buffer.readLong();
	}

	public int getTimestamp() {
		return timestamp;
	}

	public long getRandom() {
		return random;
	}

	public void writeTo(final ByteBuf buffer) {
		buffer.writeInt(timestamp);
		buffer.writeLong(random);
	}

	public String toHexString() {
		// java9 下更高效
		byte[] bytes = new byte[24];

		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 0, byteToHexLE(timestamp >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 2, byteToHexLE(timestamp >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 4, byteToHexLE(timestamp >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 6, byteToHexLE(timestamp));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 8, byteToHexLE(random >>> 56));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 10, byteToHexLE(random >>> 48));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 12, byteToHexLE(random >>> 40));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 14, byteToHexLE(random >>> 32));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 16, byteToHexLE(random >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 18, byteToHexLE(random >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 20, byteToHexLE(random >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 22, byteToHexLE(random));

		String hex = UnsafeStringUtils.toLatin1String(bytes);

		return hex;
	}

	/**
	 * 128位编码, 长度为32, 前8个字符用'0'填充
	 * 
	 * @return
	 */
	public String toHexString32() {
		byte[] bytes = new byte[32];

		unsafe().putLong(bytes, ARRAY_BYTE_BASE_OFFSET, HEX32_PADDING);
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 0 + 8, byteToHexLE(timestamp >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 2 + 8, byteToHexLE(timestamp >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 4 + 8, byteToHexLE(timestamp >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 6 + 8, byteToHexLE(timestamp));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 8 + 8, byteToHexLE(random >>> 56));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 10 + 8, byteToHexLE(random >>> 48));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 12 + 8, byteToHexLE(random >>> 40));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 14 + 8, byteToHexLE(random >>> 32));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 16 + 8, byteToHexLE(random >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 18 + 8, byteToHexLE(random >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 20 + 8, byteToHexLE(random >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 22 + 8, byteToHexLE(random));

		String hex = UnsafeStringUtils.toLatin1String(bytes);

		return hex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (random ^ (random >>> 32));
		result = prime * result + timestamp;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		RandomId other = (RandomId) obj;
		if (random != other.random)
			return false;

		if (timestamp != other.timestamp)
			return false;

		return true;
	}

	@Override
	public String toString() {
		return toHexString();
	}

	private static int currentSeconds() {
		return (int) (SystemClock.fast().seconds() - OFFSET_SECONDS);
	}

	static {
		if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
			throw new Error("only support little-endian!");
		}
	}

	public static void main(String[] args) {
		RandomId id = RandomId.next();

		System.out.println(id.toHexString());
	}

}
