package rpc.abg.invoke;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;

import rpc.abg.annotation.AbgService;

public interface InvokerUtils {

	/**
	 * 
	 * @param group
	 *            不能包含# /
	 * @param app
	 *            不能包含# /
	 * @param clazz
	 *            不能为空
	 * 
	 * @return
	 */
	public static String getServiceClassName(String group, String app, Class<?> clazz) {
		if (group == null || group.trim().length() == 0) {
			group = AbgService.DEFAULT_GROUP;
		}

		if (app == null || app.trim().length() == 0) {
			app = AbgService.DEFAULT_GROUP;
		}

		check(group, app);

		return group + "#" //
				+ app + "#" //
				+ clazz.getName();
	}

	/**
	 * 
	 * @param group
	 *            不能包含# /
	 * @param app
	 *            不能包含# /
	 * @param method
	 *            不能为空
	 * 
	 * @return
	 */
	public static String getServiceMethodName(String group, String app, Method method) {

		if (group == null || group.trim().length() == 0) {
			group = AbgService.DEFAULT_GROUP;
		}

		if (app == null || app.trim().length() == 0) {
			app = AbgService.DEFAULT_GROUP;
		}

		check(group, app);

		String version = AbgService.DEFAULT_VERSION;

		AbgService config = method.getAnnotation(AbgService.class);
		if (config == null) {
			config = method.getDeclaringClass().getAnnotation(AbgService.class);
		}

		if (config != null) {
			version = config.version();
		}

		int delimterIndex = version.indexOf('.');
		if (delimterIndex > 0) {
			version = version.substring(0, delimterIndex);
		}

		Class<?>[] parameterTypes = method.getParameterTypes();

		String params = Stream.of(parameterTypes)//
				.map(clazz -> clazz.getName())//
				.collect(Collectors.joining(",", "(", ")"));

		return group + "#" //
				+ app + "#" //
				+ method.getDeclaringClass().getName() //
				+ "#" + method.getName()//
				+ "#" + params//
				+ "#v" + version;
	}

	private static void check(String group, String app) {
		if (group.contains("#")) {
			throw new RuntimeException("group不能包含#");
		}

		if (group.contains("/")) {
			throw new RuntimeException("group不能包含/");
		}

		if (app.contains("#")) {
			throw new RuntimeException("app不能包含#");
		}

		if (app.contains("/")) {
			throw new RuntimeException("app不能包含/");
		}
	}

	/**
	 * 获取rest访问路径
	 * 
	 * @param method
	 *            不能为空
	 * 
	 * @return 当未配置或未正确配置时返回null
	 */
	public static String getRestPath(Method method) {

		AbgService methodConfig = method.getAnnotation(AbgService.class);

		if (methodConfig == null) {
			return null;
		}

		String version = methodConfig.version();
		int delimterIndex = version.indexOf('.');
		if (delimterIndex > 0) {
			version = version.substring(0, delimterIndex);
		}

		String restPath = methodConfig.rest();

		if (Strings.isNullOrEmpty(restPath)) {
			return null;
		}

		// 该方法getRestPath低频使用，没必要考虑性能问题
		while (restPath.startsWith("/")) {
			restPath = restPath.substring(1);
		}

		while (restPath.endsWith("/")) {
			restPath = restPath.substring(0, restPath.length() - 1);
		}

		AbgService classConfig = method.getDeclaringClass().getAnnotation(AbgService.class);
		if (classConfig != null) {
			String classRestPath = classConfig.rest();

			if (!Strings.isNullOrEmpty(classRestPath)) {
				while (classRestPath.startsWith("/")) {
					classRestPath = classRestPath.substring(1);
				}

				while (classRestPath.endsWith("/")) {
					classRestPath = classRestPath.substring(0, classRestPath.length() - 1);
				}

				restPath = classRestPath + "/" + restPath;
			}
		}

		restPath = restPath.replace("//", "/");

		return restPath + "?v=" + version;
	}

	public static Class<?> toClass(String serviceClassName) {
		try {
			return Class.forName(serviceClassName.split("#")[2]);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static Method toMethod(String serviceMethodName) {
		try {
			String[] array = serviceMethodName.split("#");

			String className = array[2];
			String methodName = array[3];
			String params = array[4];

			Class<?> clazz = Class.forName(className);

			for (Method method : clazz.getMethods()) {
				if (!method.getName().equals(methodName)) {
					continue;
				}

				String _params = Stream.of(method.getParameterTypes())//
						.map(c -> c.getName())//
						.collect(Collectors.joining(",", "(", ")"));

				if (_params.equals(params)) {
					return method;
				}
			}

			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
