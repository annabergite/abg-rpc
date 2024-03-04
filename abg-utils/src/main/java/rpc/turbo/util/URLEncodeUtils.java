package rpc.abg.util;

import static rpc.abg.util.UnsafeUtils.unsafe;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import rpc.abg.util.concurrent.ThreadLocalHeapByteBuffer;

/**
 * 高性能，基于HexUtils实现
 * 
 * @author Annabergite
 *
 */
public class URLEncodeUtils {
	private static final byte ESCAPE_CHAR = '%';
	private static final boolean[] SAFE_CHAR = new boolean[128];

	// Static initializer for SAFE_CHAR
	static {
		if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
			throw new Error("only support little-endian!");
		}

		// alpha characters
		for (int i = 'a'; i <= 'z'; i++) {
			SAFE_CHAR[i] = true;
		}

		for (int i = 'A'; i <= 'Z'; i++) {
			SAFE_CHAR[i] = true;
		}

		// numeric characters
		for (int i = '0'; i <= '9'; i++) {
			SAFE_CHAR[i] = true;
		}

		// special chars
		SAFE_CHAR['-'] = true;
		SAFE_CHAR['_'] = true;
		SAFE_CHAR['.'] = true;
		SAFE_CHAR['*'] = true;
		// blank to be replaced with +
		SAFE_CHAR[' '] = true;
	}

	private static final ByteBuffer decode(final byte[] bytes) {
		if (bytes == null) {
			return null;
		}

		final ByteBuffer byteBuffer = ThreadLocalHeapByteBuffer.current();

		try {
			for (int i = 0; i < bytes.length; i++) {
				final byte b = bytes[i];

				if (b == '+') {
					byteBuffer.put((byte) ' ');
				} else if (b == ESCAPE_CHAR) {

					short hex = unsafe().getShort(bytes, ARRAY_BYTE_BASE_OFFSET + i + 1);
					byteBuffer.put(HexUtils.hexToByteLE(hex));

					i += 2;
				} else {
					byteBuffer.put(b);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Invalid URL encoding: ", e);
		}

		return byteBuffer;
	}

	public static String decode(String str, Charset charset) {
		if (str == null) {
			return null;
		}

		if (!UnsafeStringUtils.isLatin1(str)) {
			throw new RuntimeException("Invalid URL encoding: " + str);
		}

		byte[] bytes = UnsafeStringUtils.getLatin1Bytes(str);
		ByteBuffer byteBuffer = decode(bytes);

		byteBuffer.flip();

		int length = byteBuffer.remaining();
		bytes = byteBuffer.array();

		return new String(bytes, 0, length, charset);
	}

	private static final ByteBuffer encode(final byte[] bytes) {
		if (bytes == null) {
			return null;
		}

		final ByteBuffer byteBuffer = ThreadLocalHeapByteBuffer.current();

		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];

			if (b > 0 && SAFE_CHAR[b]) {
				if (b == ' ') {
					b = '+';
				}

				byteBuffer.put(b);
			} else {
				byteBuffer.put(ESCAPE_CHAR);
				byteBuffer.putShort(HexUtils.byteToHex(b));
			}
		}

		return byteBuffer;
	}

	/**
	 * UTF-8
	 * 
	 * @param str
	 * @return
	 */
	public static String encode(String str) {
		if (str == null) {
			return null;
		}

		byte[] bytes = UnsafeStringUtils.getUTF8Bytes(str);
		ByteBuffer byteBuffer = encode(bytes);

		byteBuffer.flip();

		int length = byteBuffer.remaining();
		bytes = byteBuffer.array();

		return new String(bytes, 0, length, StandardCharsets.ISO_8859_1);
	}

	public static void main(String[] args) {
		System.out.println(encode("aZ Hello, World, 你好，世界！   +++ ---- %%%% @@@"));
		System.out.println(decode(
				"aZ+Hello%2c+World%2C+%E4%BD%A0%E5%A5%BD%EF%BC%8C%E4%B8%96%E7%95%8C%EF%BC%81+++%2B%2B%2B+----+%25%25%25%25+%40%40%40",
				StandardCharsets.UTF_8));
	}
}
