package rpc.abg.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 强制类型转换，把String转换为各种指定类型
 * 
 * @author Annabergite
 *
 */
public class TypeUtils {

	public static boolean supportCast(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}

		if (clazz == String.class) {
			return true;
		}

		if (clazz == int.class) {
			return true;
		}

		if (clazz == long.class) {
			return true;
		}

		if (clazz == boolean.class) {
			return true;
		}

		if (clazz == double.class) {
			return true;
		}

		if (clazz == float.class) {
			return true;
		}

		if (clazz == short.class) {
			return true;
		}

		if (clazz == byte.class) {
			return true;
		}

		if (clazz == char.class) {
			return true;
		}

		if (clazz == LocalDate.class) {
			return true;
		}

		if (clazz == LocalDateTime.class) {
			return true;
		}

		if (clazz == Integer.class) {
			return true;
		}

		if (clazz == Long.class) {
			return true;
		}

		if (clazz == Boolean.class) {
			return true;
		}

		if (clazz == Double.class) {
			return true;
		}

		if (clazz == Float.class) {
			return true;
		}

		if (clazz == Short.class) {
			return true;
		}

		if (clazz == Byte.class) {
			return true;
		}

		if (clazz == Character.class) {
			return true;
		}

		if (clazz == CharSequence.class) {
			return true;
		}

		if (clazz == BigInteger.class) {
			return true;
		}

		if (clazz == BigDecimal.class) {
			return true;
		}

		return false;
	}

	public static Object castTo(String str, Class<?> clazz) {
		if (str == null) {
			return null;
		}

		if (clazz == String.class) {
			return castToString(str);
		}

		if (clazz == int.class) {
			return castToInteger(str);
		}

		if (clazz == long.class) {
			return castToLong(str);
		}

		if (clazz == boolean.class) {
			return castToBoolean(str);
		}

		if (clazz == double.class) {
			return castToDouble(str);
		}

		if (clazz == float.class) {
			return castToFloat(str);
		}

		if (clazz == short.class) {
			return castToShort(str);
		}

		if (clazz == byte.class) {
			return castToByte(str);
		}

		if (clazz == char.class) {
			return castToCharacter(str);
		}

		if (clazz == LocalDate.class) {
			return castToLocalDate(str);
		}

		if (clazz == LocalDateTime.class) {
			return castToLocalDateTime(str);
		}

		if (clazz == Integer.class) {
			return castToInteger(str);
		}

		if (clazz == Long.class) {
			return castToLong(str);
		}

		if (clazz == Boolean.class) {
			return castToBoolean(str);
		}

		if (clazz == Double.class) {
			return castToDouble(str);
		}

		if (clazz == Float.class) {
			return castToFloat(str);
		}

		if (clazz == Short.class) {
			return castToShort(str);
		}

		if (clazz == Byte.class) {
			return castToByte(str);
		}

		if (clazz == Character.class) {
			return castToCharacter(str);
		}

		if (clazz == CharSequence.class) {
			return castToCharSequence(str);
		}

		if (clazz == BigInteger.class) {
			return castToBigInteger(str);
		}

		if (clazz == BigDecimal.class) {
			return castToBigDecimal(str);
		}

		return null;
	}

	private static DateTimeFormatter shortDateFromatter = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static LocalDate castToLocalDate(String str) {
		if (str.length() == 10) {
			return LocalDate.parse(str);
		} else if (str.length() == 8) {
			return LocalDate.parse(str, shortDateFromatter);
		} else {
			throw new IllegalArgumentException("不支持的格式: " + str);
		}
	}

	private static LocalDateTime castToLocalDateTime(String str) {
		if (str.length() == 19) {
			if (str.charAt(10) == 'T') {
				return LocalDateTime.parse(str);
			} else {
				LocalDate date = LocalDate.parse(str.substring(0, 10));
				LocalTime time = LocalTime.parse(str.substring(11));

				return LocalDateTime.of(date, time);
			}
		} else {
			throw new IllegalArgumentException("不支持的格式: " + str);
		}
	}

	public static Integer castToInteger(String value) {
		return Integer.valueOf(value);
	}

	public static Long castToLong(String value) {
		return Long.valueOf(value);
	}

	public static Boolean castToBoolean(String value) {
		if (isNumber(value)) {
			return castToInteger(value) == 0 ? Boolean.FALSE : Boolean.TRUE;
		}

		return Boolean.valueOf(value);
	}

	public static Double castToDouble(String value) {
		return Double.valueOf(value);
	}

	public static Float castToFloat(String value) {
		return Float.valueOf(value);
	}

	public static Short castToShort(String value) {
		return Short.valueOf(value);
	}

	public static Byte castToByte(String value) {
		return Byte.valueOf(value);
	}

	public static Character castToCharacter(String value) {
		return Character.valueOf(value.charAt(0));
	}

	public static String castToString(String value) {
		return URLEncodeUtils.decode(value, StandardCharsets.UTF_8);
	}

	public static CharSequence castToCharSequence(String value) {
		return URLEncodeUtils.decode(value, StandardCharsets.UTF_8);
	}

	public static BigInteger castToBigInteger(String value) {
		return new BigInteger(value);
	}

	public static BigDecimal castToBigDecimal(String value) {
		return new BigDecimal(value);
	}

	private static boolean isNumber(String str) {
		for (int i = 0; i < str.length(); ++i) {
			char ch = str.charAt(i);
			if (ch == '+' || ch == '-') {
				if (i != 0) {
					return false;
				} else {
					continue;
				}
			} else if (ch < '0' || ch > '9') {
				return false;
			}
		}
		return true;
	}
}
