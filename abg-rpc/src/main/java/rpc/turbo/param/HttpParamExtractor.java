package rpc.abg.param;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import rpc.abg.invoke.Invoker;
import rpc.abg.serialization.JsonMapper;
import rpc.abg.util.TypeUtils;
import rpc.abg.util.URLEncodeUtils;

public class HttpParamExtractor {

	private static enum JsonExtractMode {
		MIX, METHOD_PARAM, SINGLE_DIRECT_PARAM
	}

	public static Object extractFromBody(Invoker<?> invoker, JsonMapper jsonMapper, ByteBuf byteBuf) {
		Objects.requireNonNull(invoker, "invoker require non null");

		if (invoker.getParameterTypes().length == 0) {
			return null;// 无参方法
		}

		byte firstByte = byteBuf.getByte(byteBuf.readerIndex());

		if (firstByte == '{' || firstByte == '[') {
			return extractFromBodyByJson(invoker, jsonMapper, byteBuf);
		} else {
			return extractFromBodyByForm(invoker, byteBuf);
		}
	}

	public static Object extractFromBodyByJson(Invoker<?> invoker, JsonMapper jsonMapper, ByteBuf byteBuf) {
		Objects.requireNonNull(invoker, "invoker require non null");

		JsonExtractMode mode = JsonExtractMode.MIX;
		Class<?>[] parameterTypes = invoker.getParameterTypes();

		if (parameterTypes.length != 1) {// 不是单个参数只能为MethodParam
			mode = JsonExtractMode.METHOD_PARAM;
		}

		if (mode == JsonExtractMode.MIX && TypeUtils.supportCast(parameterTypes[0])) {// 基本类型只能为MethodParam
			mode = JsonExtractMode.METHOD_PARAM;
		}

		if (mode == JsonExtractMode.MIX && byteBuf.getByte(byteBuf.readerIndex()) == '[') {// 数组不能为MethodParam，只能直接映射
			mode = JsonExtractMode.SINGLE_DIRECT_PARAM;
		}

		if (mode == JsonExtractMode.MIX) {
			String parameterName = invoker.getParameterNames()[0];

			int indexStart = byteBuf.readerIndex() + 1;
			int indexEnd = byteBuf.readerIndex() + byteBuf.readableBytes();

			for (int i = indexStart; i < indexEnd; i++) {
				byte b = byteBuf.getByte(i);

				if (b == '\"') {
					for (int j = 0; j < parameterName.length(); j++) {
						if (parameterName.charAt(j) != byteBuf.getByte(i + j + 1)) {
							mode = JsonExtractMode.SINGLE_DIRECT_PARAM;
							break;
						}
					}

					if (mode == JsonExtractMode.MIX && byteBuf.getByte(i + parameterName.length() + 1) != '\"') {
						mode = JsonExtractMode.SINGLE_DIRECT_PARAM;
					}

					break;
				}
			}
		}

		if (mode == JsonExtractMode.METHOD_PARAM) {
			try {
				return (MethodParam) jsonMapper.read(byteBuf, invoker.getMethodParamClass());
			} catch (Exception e) {
				throw new RuntimeException("cannot match params: " + Arrays.toString(invoker.getParameterNames()));
			}
		} else if (mode == JsonExtractMode.SINGLE_DIRECT_PARAM) {
			try {
				return jsonMapper.read(byteBuf, parameterTypes[0]);
			} catch (Exception e) {
				throw new RuntimeException("cannot match params: " + Arrays.toString(invoker.getParameterNames()));
			}
		} else {
			int index = byteBuf.readerIndex();

			try {
				return jsonMapper.read(byteBuf, parameterTypes[0]);
			} catch (Exception e) {
				byteBuf.readerIndex(index);

				try {
					return (MethodParam) jsonMapper.read(byteBuf, invoker.getMethodParamClass());
				} catch (Exception e2) {
					throw new RuntimeException("cannot match params: " + Arrays.toString(invoker.getParameterNames()));
				}
			}
		}
	}

	public static Object[] extractFromBodyByForm(Invoker<?> invoker, ByteBuf byteBuf) {
		Objects.requireNonNull(invoker, "invoker require non null");

		if (!invoker.supportHttpForm()) {
			throw new IllegalAccessError(
					"HTTP POST FORM only support primitive-data-types and String, please use POST-JSON");
		}

		Class<?>[] parameterTypes = invoker.getParameterTypes();
		String[] parameterNames = invoker.getParameterNames();

		if (parameterTypes.length == 0) {
			return null;// 无参方法
		}

		if (parameterTypes.length > 8) {
			throw new IllegalAccessError("HTTP POST FORM max support 8 params, please use POST-JSON");
		}

		Object[] params = new Object[parameterTypes.length];

		int offset = byteBuf.readerIndex();
		int begin = byteBuf.readerIndex();
		int end = offset + byteBuf.readableBytes();

		int number = 0;
		while (true) {
			int indexBegin = offset;
			if (offset > begin) {
				indexBegin = byteBuf.indexOf(offset, end, (byte) '&');
			}

			if (indexBegin < 0) {
				break;
			}

			indexBegin += 1;
			offset = indexBegin;

			int indexEnd = byteBuf.indexOf(offset, end, (byte) '=');
			indexEnd = indexEnd < 0 ? end : indexEnd;

			int length = indexEnd - indexBegin;
			String key = byteBuf.getCharSequence(indexBegin, length, StandardCharsets.US_ASCII).toString();

			key = URLEncodeUtils.decode(key, StandardCharsets.UTF_8);

			int paramIndex = indexOf(parameterNames, key);

			if (paramIndex < 0) {
				continue;
			}

			indexBegin = indexEnd + 1;
			offset = indexBegin;
			indexEnd = byteBuf.indexOf(offset, end, (byte) '&');

			indexEnd = indexEnd < 0 ? end : indexEnd;
			offset = indexEnd;

			length = indexEnd - indexBegin;
			String value = byteBuf.getCharSequence(indexBegin, length, StandardCharsets.US_ASCII).toString();

			Object param = TypeUtils.castTo(value, parameterTypes[paramIndex]);
			params[paramIndex] = param;

			if (++number == parameterTypes.length) {
				break;
			}
		}

		if (number != parameterTypes.length) {
			throw new IllegalAccessError("error params count, must contains all: " + Arrays.toString(parameterNames));
		}

		return params;
	}

	public static Object[] extractFromQueryPath(Invoker<?> invoker, String uri, int offset) {
		Objects.requireNonNull(invoker, "invoker require non null");
		Objects.requireNonNull(uri, "uri require non null");

		if (!invoker.supportHttpForm()) {
			throw new IllegalAccessError("HTTP GET only support primitive-data-types and String, please use POST-JSON");
		}

		Class<?>[] parameterTypes = invoker.getParameterTypes();
		String[] parameterNames = invoker.getParameterNames();

		if (parameterTypes.length == 0) {
			return null;// 无参方法
		}

		if (parameterTypes.length > 8) {
			throw new IllegalAccessError("HTTP GET max support 8 params, please use POST-JSON");
		}

		Object[] params = new Object[parameterTypes.length];

		if (offset < 0) {
			return params;// 抽取不到参数
		}

		int number = 0;
		while (true) {
			int indexBegin = uri.indexOf('&', offset);

			if (indexBegin < 0) {
				break;
			}

			indexBegin += 1;
			offset = indexBegin;

			int indexEnd = uri.indexOf('=', offset);
			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			String key = uri.substring(indexBegin, indexEnd);
			key = URLEncodeUtils.decode(key, StandardCharsets.UTF_8);

			int paramIndex = indexOf(parameterNames, key);

			if (paramIndex < 0) {
				continue;
			}

			indexBegin = indexEnd + 1;
			offset = indexBegin;
			indexEnd = uri.indexOf('&', offset);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;
			offset = indexEnd;

			String value = uri.substring(indexBegin, indexEnd);

			Object param = TypeUtils.castTo(value, parameterTypes[paramIndex]);
			params[paramIndex] = param;

			if (++number == parameterTypes.length) {
				break;
			}
		}

		if (number != parameterTypes.length) {
			throw new IllegalAccessError("error params count, must contains all: " + Arrays.toString(parameterNames));
		}

		return params;
	}

	/**
	 * @param array
	 *            不为null，不含null元素
	 * @param value
	 *            不为null
	 * @return
	 */
	private static int indexOf(Object[] array, Object value) {
		for (int i = 0; i < array.length; i++) {
			Object object = array[i];

			if (object.equals(value)) {
				return i;
			}
		}

		return -1;
	}
}
