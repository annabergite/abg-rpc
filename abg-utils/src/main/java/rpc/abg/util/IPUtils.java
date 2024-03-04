package rpc.abg.util;

public class IPUtils {

	/**
	 * 
	 * @param ip
	 *            ipv4 only
	 * 
	 * @return 找不到返回-1
	 */
	public static final long ipToLong(final String ip) {
		final int strLen = ip.length();
		if (strLen == 0 || strLen > 15) {
			return -1L;
		}

		int result = 0;
		int item = 0;
		int shift = 3 * 8;

		int index = 0;
		while (true) {
			char c = ip.charAt(index++);

			if (c != '.') {// 处理每个单元数字
				int digit = c - 48;

				if (digit < 0 || digit > 9) {// 有效性检查，每个字符都必须为0-9的个位数字
					return -1L;
				}

				item = (item << 3) + (item << 1) + digit;
			} else {// 将单元数字移位后加到结果里
				if (item > 255) {// 有效性检查，每单元必须为0-255
					return -1L;
				}

				result |= (item << shift);

				item = 0;
				shift -= 8;
			}

			if (index == strLen) {// 处理最后一个单元数字
				if (item > 255) {// 有效性检查，每单元必须为0-255
					return -1L;
				}

				result |= item;

				if (shift == 0) {// 有效性检查，分割点的数量必须为3个
					return result & 0xFFFFFFFFL;
				} else {
					return -1L;
				}
			}
		}
	}

	/**
	 * 
	 * @param ip
	 *            ipv4 only
	 * 
	 * @return 找不到返回0，需要用户自己特殊处理0.0.0.0的情况
	 */
	public static final int ipToInt(final String ip) {
		long ipLong = ipToLong(ip);

		if (ipLong != -1L) {
			return (int) ipLong;
		} else {
			return 0;
		}
	}

}
