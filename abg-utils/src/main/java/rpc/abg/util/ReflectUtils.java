package rpc.abg.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class ReflectUtils {

	/**
	 * 获取字段的get方法
	 * 
	 * @param targetClass
	 * @param field
	 * @return
	 */
	public static Method getGetMethod(Class<?> targetClass, Field field) {
		return getGetMethod(targetClass.getMethods(), field);
	}

	/**
	 * 获取字段的get方法
	 * 
	 * @param methods
	 * @param field
	 * @return
	 */
	public static Method getGetMethod(Method[] methods, Field field) {
		String name = field.getName();
		Class<?> fieldType = field.getType();

		String getMethodName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);

		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];

			if (method.getParameterCount() > 0) {
				continue;
			}

			String methodName = method.getName();

			if (!methodName.equals(name) && !methodName.equals(getMethodName)) {
				continue;
			}

			if (!fieldType.isAssignableFrom(method.getReturnType())) {
				continue;
			}

			return method;
		}

		return null;
	}

	/**
	 * 获取字段的set方法
	 * 
	 * @param targetClass
	 * @param field
	 * @return
	 */
	public static Method getSetMethod(Class<?> targetClass, Field field) {
		return getSetMethod(targetClass.getMethods(), field);
	}

	/**
	 * 获取字段的set方法
	 * 
	 * @param methods
	 * @param field
	 * @return
	 */
	public static Method getSetMethod(Method[] methods, Field field) {
		String name = field.getName();
		Class<?> fieldType = field.getType();

		String getMethodName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];

			if (method.getParameterCount() != 1) {
				continue;
			}

			String methodName = method.getName();

			if (!methodName.equals(name) && !methodName.equals(getMethodName)) {
				continue;
			}

			if (!fieldType.isAssignableFrom(method.getParameterTypes()[0])) {
				continue;
			}

			return method;
		}

		return null;
	}

	/**
	 * 获取所有依赖的类，包括字段、方法返回值
	 * 
	 * @param target
	 * @param filter
	 *            过滤器
	 * @return
	 */
	public static Collection<Class<?>> getAllDependClass(Class<?> target, Predicate<Class<?>> filter) {
		Objects.requireNonNull(target, "target is null");

		Set<Class<?>> set = null;

		while (target != Object.class) {
			Field[] fields = target.getDeclaredFields();
			for (int j = 0; j < fields.length; j++) {
				Field field = fields[j];
				Class<?> clazz = field.getType();

				if (filter != null && !filter.test(clazz)) {
					continue;
				}

				if (set == null) {
					set = new HashSet<>();
				}

				set.add(clazz);
			}

			Method[] methods = target.getDeclaredMethods();
			for (int j = 0; j < methods.length; j++) {
				Method method = methods[j];
				Class<?> clazz = method.getReturnType();

				if (filter != null && !filter.test(clazz)) {
					continue;
				}

				if (set == null) {
					set = new HashSet<>();
				}

				set.add(clazz);
			}

			target = target.getSuperclass();
		}

		return set == null ? Set.of() : set;
	}

}
