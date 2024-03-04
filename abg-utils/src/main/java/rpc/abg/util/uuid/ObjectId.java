package rpc.abg.util.uuid;

import static java.time.ZoneOffset.UTC;
import static rpc.abg.util.HexUtils.byteToHexLE;
import static rpc.abg.util.UnsafeUtils.unsafe;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import rpc.abg.util.SystemClock;
import rpc.abg.util.UnsafeStringUtils;
import rpc.abg.util.concurrent.ConcurrentIntegerSequencer;

/**
 * <p>
 * A globally unique identifier for objects.
 * </p>
 *
 * <p>
 * Consists of 12 bytes, divided as follows:
 * </p>
 * <table border="1">
 * <caption>ObjectID layout</caption>
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
 * <td colspan="3">machine</td>
 * <td colspan="2">pid</td>
 * <td colspan="3">inc</td>
 * </tr>
 * </table>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 *
 */
public final class ObjectId implements Serializable {

	private static final long serialVersionUID = -3433285147990005311L;

	private static final int LOW_ORDER_THREE_BYTES = 0x00FFFFFF;

	public final static long OFFSET_SECONDS = LocalDateTime.parse("2018-01-01T00:00:00").toEpochSecond(UTC);

	private static final long HEX32_PADDING = ByteBuffer.wrap("0000000000000000".getBytes()).getLong();

	private static final int MACHINE_IDENTIFIER;
	private static final short PROCESS_IDENTIFIER;
	private static final long UNION_HIGH;

	private static final ConcurrentIntegerSequencer NEXT_COUNTER = new ConcurrentIntegerSequencer(
			new SecureRandom().nextInt());

	public final int timestamp;
	public final long union;

	/**
	 * Gets a new object id.
	 *
	 * @return the new id
	 */
	public static ObjectId next() {
		int next = NEXT_COUNTER.next() & LOW_ORDER_THREE_BYTES;
		long union = UNION_HIGH | next;

		return new ObjectId(currentSeconds(), union);
	}

	public ObjectId(final int timestamp, final long union) {
		this.timestamp = timestamp;
		this.union = union;
	}

	public ObjectId(final ByteBuf buffer) {
		Objects.requireNonNull(buffer, "buffer");

		timestamp = buffer.readInt();
		union = buffer.readLong();
	}

	public int getTimestamp() {
		return timestamp;
	}

	public long getUnion() {
		return union;
	}

	public void writeTo(final ByteBuf buffer) {
		buffer.writeInt(timestamp);
		buffer.writeLong(union);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + timestamp;
		result = prime * result + (int) (union ^ (union >>> 32));
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
		ObjectId other = (ObjectId) obj;
		if (timestamp != other.timestamp)
			return false;
		if (union != other.union)
			return false;
		return true;
	}

	/**
	 * 96位编码, 长度为24
	 * 
	 * @return
	 */
	public String toHexString() {
		byte[] bytes = new byte[24];

		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 0, byteToHexLE(timestamp >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 2, byteToHexLE(timestamp >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 4, byteToHexLE(timestamp >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 6, byteToHexLE(timestamp));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 8, byteToHexLE(union >>> 56));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 10, byteToHexLE(union >>> 48));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 12, byteToHexLE(union >>> 40));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 14, byteToHexLE(union >>> 32));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 16, byteToHexLE(union >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 18, byteToHexLE(union >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 20, byteToHexLE(union >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 22, byteToHexLE(union));

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
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 8 + 8, byteToHexLE(union >>> 56));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 10 + 8, byteToHexLE(union >>> 48));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 12 + 8, byteToHexLE(union >>> 40));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 14 + 8, byteToHexLE(union >>> 32));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 16 + 8, byteToHexLE(union >>> 24));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 18 + 8, byteToHexLE(union >>> 16));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 20 + 8, byteToHexLE(union >>> 8));
		unsafe().putShort(bytes, ARRAY_BYTE_BASE_OFFSET + 22 + 8, byteToHexLE(union));

		String hex = UnsafeStringUtils.toLatin1String(bytes);

		return hex;
	}

	@Override
	public String toString() {
		return toHexString();
	}

	static {
		if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
			throw new Error("only support little-endian!");
		}

		try {
			MACHINE_IDENTIFIER = createMachineIdentifier();
			PROCESS_IDENTIFIER = createProcessIdentifier();

			UNION_HIGH = ((MACHINE_IDENTIFIER & 0xFF_FF_FFL) << 40) | ((PROCESS_IDENTIFIER & 0xFF_FFL) << 24);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static int createMachineIdentifier() {
		// build a 2-byte machine piece based on NICs info
		int machinePiece;
		try {
			StringBuilder sb = new StringBuilder();
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface ni = e.nextElement();
				sb.append(ni.toString());
				byte[] mac = ni.getHardwareAddress();
				if (mac != null) {
					ByteBuffer bb = ByteBuffer.wrap(mac);
					try {
						sb.append(bb.getChar());
						sb.append(bb.getChar());
						sb.append(bb.getChar());
					} catch (BufferUnderflowException shortHardwareAddressException) { // NOPMD
						// mac with less than 6 bytes. continue
					}
				}
			}
			machinePiece = sb.toString().hashCode();
		} catch (Throwable t) {
			// exception sometimes happens with IBM JVM, use random
			machinePiece = (new SecureRandom().nextInt());
			System.err.println("Failed to get machine identifier from network interface, using random number instead "
					+ Arrays.toString(t.getStackTrace()));
		}
		machinePiece = machinePiece & LOW_ORDER_THREE_BYTES;
		return machinePiece;
	}

	// Creates the process identifier. This does not have to be unique per class
	// loader because
	// NEXT_COUNTER will provide the uniqueness.
	private static short createProcessIdentifier() {
		short processId;
		try {
			String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
			if (processName.contains("@")) {
				processId = (short) Integer.parseInt(processName.substring(0, processName.indexOf('@')));
			} else {
				processId = (short) java.lang.management.ManagementFactory.getRuntimeMXBean().getName().hashCode();
			}

		} catch (Throwable t) {
			processId = (short) new SecureRandom().nextInt();

			System.err.println("Failed to get process identifier from JMX, using random number instead "
					+ Arrays.toString(t.getStackTrace()));
		}

		return processId;
	}

	private static int currentSeconds() {
		return (int) (SystemClock.fast().seconds() - OFFSET_SECONDS);
	}

	public static void main(String[] args) {
		ObjectId id = next();

		System.out.println(id.toHexString());
		System.out.println(id.toHexString32());

		while (true) {
			next();
		}
	}

}
