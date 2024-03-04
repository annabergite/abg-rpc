package rpc.abg.util;

import static rpc.abg.util.UnsafeUtils.unsafe;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * 高性能，空间换时间
 * 
 * @author Annabergite
 *
 */
public class HexUtils {

	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final short[] HEX_TABLE = new short[256];
	private static final short[] HEX_TABLE_LE = new short[256];
	private static final short[] UPPER_HEX_TABLE = new short[256];
	private static final short[] UPPER_HEX_TABLE_LE = new short[256];

	private static final byte[] BYTE_TABLE;
	private static final byte[] BYTE_TABLE_LE;

	static {
		if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
			throw new Error("only support little-endian!");
		}

		final char[] DIGITS = "0123456789abcdef".toCharArray();
		final char[] UPPER_DIGITS = "0123456789ABCDEF".toCharArray();

		for (int i = 0; i < 256; i++) {
			int high = DIGITS[(0xF0 & i) >>> 4];
			int low = DIGITS[0x0F & i];

			HEX_TABLE[i] = (short) (high << 8 | low);
			HEX_TABLE_LE[i] = (short) (low << 8 | high);

			high = UPPER_DIGITS[(0xF0 & i) >>> 4];
			low = UPPER_DIGITS[0x0F & i];

			UPPER_HEX_TABLE[i] = (short) (high << 8 | low);
			UPPER_HEX_TABLE_LE[i] = (short) (low << 8 | high);
		}

		BYTE_TABLE = new byte[HEX_TABLE[255] + 1];
		for (int i = 0; i < 256; i++) {
			BYTE_TABLE[HEX_TABLE[i]] = (byte) i;
			BYTE_TABLE[UPPER_HEX_TABLE[i]] = (byte) i;
		}

		BYTE_TABLE_LE = new byte[HEX_TABLE_LE[255] + 1];
		for (int i = 0; i < 256; i++) {
			BYTE_TABLE_LE[HEX_TABLE_LE[i]] = (byte) i;
			BYTE_TABLE_LE[UPPER_HEX_TABLE_LE[i]] = (byte) i;
		}
	}

	public static short byteToHex(byte b) {
		return HEX_TABLE[b & 0xFF];
	}

	public static short byteToHex(int b) {
		return HEX_TABLE[b & 0xFF];
	}

	public static short byteToHex(long b) {
		return HEX_TABLE[(int) (b & 0xFF)];
	}

	public static short byteToHexLE(byte b) {
		return HEX_TABLE_LE[b & 0xFF];
	}

	public static short byteToHexLE(int b) {
		return HEX_TABLE_LE[b & 0xFF];
	}

	public static short byteToHexLE(long b) {
		return HEX_TABLE_LE[((int) b) & 0xFF];
	}

	public static byte hexToByte(short hex) {
		return BYTE_TABLE[hex];
	}

	public static byte hexToByteLE(short hex) {
		return BYTE_TABLE_LE[hex];
	}

	public static byte hexToByte(long hex) {
		return BYTE_TABLE[(int) (hex & 0xFFFF)];
	}

	public static byte hexToByteLE(long hex) {
		return BYTE_TABLE_LE[(int) (hex & 0xFFFF)];
	}

	/**
	 * 略慢
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toHex2(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes is null");

		int length = bytes.length;
		if (length == 0) {
			return "";
		}

		byte[] hexBytes = new byte[length << 1];

		for (int i = 0; i < length; i++) {
			byte b = bytes[i];
			unsafe().putShort(hexBytes, ARRAY_BYTE_BASE_OFFSET + i * 2, byteToHexLE(b));
		}

		return UnsafeStringUtils.toLatin1String(hexBytes);
	}

	/**
	 * high performance
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toHex(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes is null");

		int length = bytes.length;
		if (length == 0) {
			return "";
		}

		byte[] hexBytes = new byte[length << 1];

		int batchLength = length >>> 3 << 3;

		for (int i = 0; i < batchLength; i += 8) {
			long offset = ARRAY_BYTE_BASE_OFFSET + i;
			long bytesLong = unsafe().getLong(bytes, offset);

			long hex;

			hex = ((byteToHexLE(bytesLong) & 0xFFFFL));
			hex |= ((byteToHexLE(bytesLong >>> 8 * 1) & 0xFFFFL) << 8 * 2);
			hex |= ((byteToHexLE(bytesLong >>> 8 * 2) & 0xFFFFL) << 8 * 4);
			hex |= ((byteToHexLE(bytesLong >>> 8 * 3) & 0xFFFFL) << 8 * 6);

			offset += i;
			unsafe().putLong(hexBytes, offset, hex);

			hex = ((byteToHexLE(bytesLong >>> 8 * 4) & 0xFFFFL));
			hex |= ((byteToHexLE(bytesLong >>> 8 * 5) & 0xFFFFL) << 8 * 2);
			hex |= ((byteToHexLE(bytesLong >>> 8 * 6) & 0xFFFFL) << 8 * 4);
			hex |= ((byteToHexLE(bytesLong >>> 8 * 7) & 0xFFFFL) << 8 * 6);

			unsafe().putLong(hexBytes, offset + 8, hex);
		}

		for (int i = batchLength; i < length; i++) {
			unsafe().putShort(hexBytes, ARRAY_BYTE_BASE_OFFSET + (i << 1), byteToHexLE(bytes[i]));
		}

		return UnsafeStringUtils.toLatin1String(hexBytes);
	}

	public static void toHex(byte[] bytes, int offset, int value) {
		long hex;

		hex = ((byteToHexLE(value) & 0xFFFFL) << 8 * 6);
		hex |= ((byteToHexLE(value >>> 8 * 1) & 0xFFFFL) << 8 * 4);
		hex |= ((byteToHexLE(value >>> 8 * 2) & 0xFFFFL) << 8 * 2);
		hex |= ((byteToHexLE(value >>> 8 * 3) & 0xFFFFL));

		unsafe().putLong(bytes, ARRAY_BYTE_BASE_OFFSET + offset, hex);
	}

	public static void toHex(byte[] bytes, int offset, long value) {
		long hex;

		hex = ((byteToHexLE(value) & 0xFFFFL) << 8 * 6);
		hex |= ((byteToHexLE(value >>> 8 * 1) & 0xFFFFL) << 8 * 4);
		hex |= ((byteToHexLE(value >>> 8 * 2) & 0xFFFFL) << 8 * 2);
		hex |= ((byteToHexLE(value >>> 8 * 3) & 0xFFFFL));
		unsafe().putLong(bytes, ARRAY_BYTE_BASE_OFFSET + offset + 8, hex);

		hex = ((byteToHexLE(value >>> 8 * 4) & 0xFFFFL) << 8 * 6);
		hex |= ((byteToHexLE(value >>> 8 * 5) & 0xFFFFL) << 8 * 4);
		hex |= ((byteToHexLE(value >>> 8 * 6) & 0xFFFFL) << 8 * 2);
		hex |= ((byteToHexLE(value >>> 8 * 7) & 0xFFFFL));
		unsafe().putLong(bytes, ARRAY_BYTE_BASE_OFFSET + offset, hex);
	}

	public static String toHex(int value) {
		byte[] bytes = new byte[8];
		toHex(bytes, 0, value);

		String hex = UnsafeStringUtils.toLatin1String(bytes);

		return hex;
	}

	public static String toHex(long value) {
		byte[] bytes = new byte[16];
		toHex(bytes, 0, value);

		String hex = UnsafeStringUtils.toLatin1String(bytes);

		return hex;
	}

	/**
	 * high performance
	 * 
	 * @param byteBuffer
	 * @param offset
	 * @param length
	 * @return
	 */
	public static String toHex(ByteBuffer byteBuffer, int offset, int length) {
		Objects.requireNonNull(byteBuffer, "byteBuffer is null");

		if (byteBuffer.remaining() + byteBuffer.position() - offset < length) {
			throw new IllegalArgumentException("no enough bytes");
		}

		byte[] hexBytes = new byte[length << 1];

		for (int i = 0; i < offset; i++) {
			byte b = byteBuffer.get(offset + i);
			unsafe().putShort(hexBytes, ARRAY_BYTE_BASE_OFFSET + i * 2, byteToHexLE(b));
		}

		return UnsafeStringUtils.toLatin1String(hexBytes);
	}

	/**
	 * high performance
	 * 
	 * @param hex
	 * @return
	 */
	public static byte[] fromHex(String hex) {
		int length = hex.length();

		if (length == 0) {
			return EMPTY_BYTES;
		}

		if ((length & 1) == 1) {// 奇数
			throw new IllegalArgumentException("not hex String");
		}

		byte[] bytes = new byte[length >>> 1];
		byte[] hexBytes = UnsafeStringUtils.getLatin1Bytes(hex);

		int batchLength = length >>> 4 << 4;
		for (int i = 0; i < batchLength; i += 16) {
			long offset = ARRAY_BYTE_BASE_OFFSET + i;

			long bytesLong = 0;
			long hexLong = unsafe().getLong(hexBytes, offset);

			bytesLong = (hexToByteLE(hexLong) & 0xFFL);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 2) & 0xFFL) << 8);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 4) & 0xFFL) << 8 * 2);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 6) & 0xFFL) << 8 * 3);

			hexLong = unsafe().getLong(hexBytes, offset + 8);
			bytesLong |= ((hexToByteLE(hexLong) & 0xFFL) << 8 * 4);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 2) & 0xFFL) << 8 * 5);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 4) & 0xFFL) << 8 * 6);
			bytesLong |= ((hexToByteLE(hexLong >>> 8 * 6) & 0xFFL) << 8 * 7);

			unsafe().putLong(bytes, ARRAY_BYTE_BASE_OFFSET + (i >>> 1), bytesLong);
		}

		for (int i = batchLength; i < length; i += 2) {
			short index = unsafe().getShort(hexBytes, ARRAY_BYTE_BASE_OFFSET + i);
			bytes[i >>> 1] = hexToByteLE(index);
		}

		return bytes;
	}

	public static byte[] fromHex2(String hex) {
		int length = hex.length();

		if (length == 0) {
			return EMPTY_BYTES;
		}

		if ((length & 1) == 1) {// 奇数
			throw new IllegalArgumentException("not hex String");
		}

		byte[] bytes = new byte[length >>> 1];
		byte[] hexBytes = UnsafeStringUtils.getLatin1Bytes(hex);

		for (int i = 0; i < length; i += 2) {
			short index = unsafe().getShort(hexBytes, ARRAY_BYTE_BASE_OFFSET + i);
			bytes[i >>> 1] = hexToByteLE(index);
		}

		return bytes;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {

		String str = "aZ Hello, World, 你好，世界！   +++ ---- %%%% @@@";
		byte[] bytes = str.getBytes("UTF-8");
		String hex = toHex(bytes);
		System.out.println(hex);

		hex = toHex2(bytes);

		System.out.println(hex);
		System.out.println(Arrays.toString(bytes));
		System.out.println(Arrays.toString(fromHex2(hex)));
		System.out.println(Arrays.toString(fromHex(hex)));

		System.out.println(Integer.toHexString(Integer.MAX_VALUE));
		System.out.println(toHex(Integer.MAX_VALUE));

		System.out.println(Long.toHexString(Long.MAX_VALUE));
		System.out.println(toHex(Long.MAX_VALUE));
	}

}
