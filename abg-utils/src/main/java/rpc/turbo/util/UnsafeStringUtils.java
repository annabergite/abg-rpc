package rpc.abg.util;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/**
 * 超高性能，用了一些黑魔法，依赖于特定的jvm实现，仅在Oracle JDK 9下有效
 * 
 * @author Annabergite
 *
 */
public class UnsafeStringUtils {

	private static final byte LATIN1 = 0;
	private static final byte UTF16 = 1;

	private static final long coderFieldOffset;
	private static final long bytesFieldOffset;
	private static final long hashFieldOffset;

	static {
		{
			long _coderFieldOffset = -1;

			try {
				Field coderField = String.class.getDeclaredField("coder");
				_coderFieldOffset = UnsafeUtils.unsafe().objectFieldOffset(coderField);

				if (UnsafeUtils.unsafe().getByte("你好，世界", _coderFieldOffset) != UTF16) {
					_coderFieldOffset = -1;
				}
			} catch (Throwable e) {
				_coderFieldOffset = -1;
			}

			coderFieldOffset = _coderFieldOffset;

			if (coderFieldOffset == -1) {
				System.err.println("StringUtils2.isLatin1(String) is broken");
			}
		}

		{
			long _bytesFieldOffset = -1;

			try {
				Field valueField = String.class.getDeclaredField("value");
				_bytesFieldOffset = UnsafeUtils.unsafe().objectFieldOffset(valueField);

				if (UnsafeUtils.unsafe().getObject("你好，世界", _bytesFieldOffset) == null) {
					_bytesFieldOffset = -1;
				}

				if (!(UnsafeUtils.unsafe().getObject("你好，世界", _bytesFieldOffset) instanceof byte[])) {
					_bytesFieldOffset = -1;
				}
			} catch (Throwable e) {
				_bytesFieldOffset = -1;
			}

			bytesFieldOffset = _bytesFieldOffset;

			if (bytesFieldOffset == -1) {
				System.err.println("StringUtils2.getUTF8Bytes(String) is broken");
			}
		}

		{
			long _hashFieldOffset = -1;

			try {
				Field hashField = String.class.getDeclaredField("hash");
				_hashFieldOffset = UnsafeUtils.unsafe().objectFieldOffset(hashField);
			} catch (Throwable e) {
				_hashFieldOffset = -1;
			}

			hashFieldOffset = _hashFieldOffset;

			if (hashFieldOffset == -1) {
				System.err.println("StringUtils2.toLatin1String(byte[]) is broken");
			}
		}

	}

	public static boolean isLatin1(String str) {
		if (coderFieldOffset > 0) {
			return UnsafeUtils.unsafe().getByte(str, coderFieldOffset) == LATIN1;
		}

		return false;
	}

	/**
	 * unsafe, do not change the return bytes
	 * 
	 * @param str
	 * @return
	 */
	public static byte[] getUTF8Bytes(String str) {
		if (bytesFieldOffset > 0 && isLatin1(str)) {
			byte[] bytes = (byte[]) UnsafeUtils.unsafe().getObject(str, bytesFieldOffset);

			if (bytes != null) {
				boolean ascii = true;

				for (int i = 0; i < bytes.length; i++) {
					if (bytes[i] < 0) {
						ascii = false;
						break;
					}
				}

				if (ascii) {
					return bytes;
				}
			}
		}

		return str.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * unsafe, do not change the return bytes
	 * 
	 * @param str
	 * @return
	 */
	public static byte[] getLatin1Bytes(String str) {
		if (bytesFieldOffset > 0 && isLatin1(str)) {
			byte[] bytes = (byte[]) UnsafeUtils.unsafe().getObject(str, bytesFieldOffset);

			if (bytes != null) {
				return bytes;
			}
		}

		return str.getBytes(StandardCharsets.ISO_8859_1);
	}

	/**
	 * very unsafe
	 * 
	 * @param bytes
	 * @return
	 */
	public static String toLatin1String(byte[] bytes) {
		if (bytesFieldOffset == -1 || coderFieldOffset == -1 || hashFieldOffset == -1) {
			return new String(bytes, StandardCharsets.ISO_8859_1);
		}

		// allocate String instance
		Object obj = null;
		try {
			obj = UnsafeUtils.unsafe().allocateInstance(String.class);
		} catch (Throwable t) {
			return new String(bytes, StandardCharsets.ISO_8859_1);
		}

		// init field
		UnsafeUtils.unsafe().putObject(obj, bytesFieldOffset, bytes);
		UnsafeUtils.unsafe().putByte(obj, coderFieldOffset, LATIN1);
		UnsafeUtils.unsafe().putInt(obj, hashFieldOffset, 0);

		return (String) obj;
	}

}
