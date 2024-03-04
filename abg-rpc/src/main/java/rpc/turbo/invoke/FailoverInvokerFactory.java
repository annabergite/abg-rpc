package rpc.abg.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import rpc.abg.util.SingleClassLoader;

/**
 * 客户端失败回退Invoker工厂类
 * 
 * @author Annabergite
 *
 */
public class FailoverInvokerFactory {
	private static final Log logger = LogFactory.getLog(FailoverInvokerFactory.class);

	// 低频使用
	private final ConcurrentMap<Method, JavassistInvoker<?>> methodInvokerMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Class<?>, Object> defaultImplObjectMap = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <T> Invoker<T> get(Method method) {
		return (Invoker<T>) methodInvokerMap.get(method);
	}

	/**
	 * 注册failvoer invoker
	 * 
	 * @param failover
	 *            松散约束可以不实现服务接口，只要方法签名一致就能自动匹配上,<br>
	 *            优先于clazz服务接口中的默认方法
	 * 
	 * @param clazz
	 *            服务接口，当接口中包含公开的默认方法时自动注册为出错回退方法
	 * 
	 */
	public void register(Class<?> clazz, Object failover) {
		convertToInvokerStream(clazz, failover)//
				.forEach(invoker -> methodInvokerMap.put(invoker.method, invoker));
	}

	private <T> Stream<JavassistInvoker<T>> convertToInvokerStream(Class<?> clazz, Object failover) {

		if (clazz == null) {
			throw new InvokeException("clazz cannot be null");
		}

		if (!clazz.isInterface()) {
			throw new InvokeException("the clazz must be interface");
		}

		if (!Modifier.isPublic(clazz.getModifiers())) {
			throw new InvokeException("the clazz must be public");
		}

		Method[] allMethods = clazz.getMethods();

		if (failover == null && !Stream.of(allMethods).anyMatch(m -> m.isDefault())) {
			return Stream.empty();
		}

		Object _defaultImplObject = defaultImplObjectMap.get(clazz);

		if (_defaultImplObject == null) {
			try {
				_defaultImplObject = generateDefaultImplObject(clazz);
				defaultImplObjectMap.put(clazz, _defaultImplObject);
			} catch (Exception e) {
				throw new InvokeException(e);
			}
		}

		final Object defaultImplObject = _defaultImplObject;

		return Stream//
				.of(allMethods)//
				.filter(m -> Modifier.isPublic(m.getModifiers()))//
				.filter(m -> !Modifier.isStatic(m.getModifiers()))//
				.peek(m -> {
					if (!CompletableFuture.class.equals(m.getReturnType())) {
						throw new RuntimeException("method return-type must be CompletableFuture, "
								+ InvokerUtils.getServiceMethodName("", "", m));
					}
				})//
				.map(m -> {
					if (failover != null) {
						Method failoverMethod = null;

						if (failover != null) {
							try {
								failoverMethod = failover.getClass().getDeclaredMethod(m.getName(),
										m.getParameterTypes());
							} catch (Throwable t) {
							}

							if (failoverMethod != null //
									&& failoverMethod.getReturnType().equals(m.getReturnType())) {
								if (logger.isInfoEnabled()) {
									String methodName = clazz.getName() + "." + m.getName();
									logger.info("成功创建failover invoker, 使用失败回退对象, method:" + methodName + ", failover:"
											+ failover.getClass().getName());
								}

								return new JavassistInvoker<T>(0, failover, m.getDeclaringClass(), m);
							}
						}
					}

					if (m.isDefault()) {
						if (logger.isInfoEnabled()) {
							String methodName = clazz.getName() + "." + m.getName();
							logger.info("成功创建failover invoker, 使用接口默认方法, method:" + methodName);
						}

						return new JavassistInvoker<T>(0, defaultImplObject, m.getDeclaringClass(), m);
					}

					return null;
				})//
				.filter(item -> item != null);
	}

	private Object generateDefaultImplObject(Class<?> clazz) throws Exception {

		if (clazz == null) {
			throw new RuntimeException("clazz must not be null");
		}

		Method[] allMethods = clazz.getMethods();

		for (Method method : allMethods) {
			if (!CompletableFuture.class.equals(method.getReturnType())) {
				throw new RuntimeException("method return-type must be CompletableFuture, " + method);
			}
		}

		if (!Stream.of(allMethods).anyMatch(m -> m.isDefault())) {
			return null;
		}

		final String remoteClassName = clazz.getName() + "_DefaultImpl_"//
				+ UUID.randomUUID().toString().replace("-", "");

		// 创建类
		ClassPool pool = ClassPool.getDefault();
		CtClass defaultImplCtClass = pool.makeClass(remoteClassName);

		CtClass[] interfaces = { pool.getCtClass(clazz.getName()) };
		defaultImplCtClass.setInterfaces(interfaces);

		// 添加无参的构造函数
		CtConstructor constructor = new CtConstructor(null, defaultImplCtClass);
		constructor.setModifiers(Modifier.PUBLIC);
		constructor.setBody("{}");
		defaultImplCtClass.addConstructor(constructor);

		for (Method method : allMethods) {

			if (method.isDefault()) {
				continue;
			}

			StringBuilder methodBuilder = new StringBuilder();

			methodBuilder.append("public ");
			methodBuilder.append(method.getReturnType().getName());
			methodBuilder.append(" ");
			methodBuilder.append(method.getName());
			methodBuilder.append("(");

			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> parameterType = parameterTypes[i];

				methodBuilder.append(parameterType.getName());
				methodBuilder.append(" param");
				methodBuilder.append(i);

				if (i != parameterTypes.length - 1) {
					methodBuilder.append(", ");
				}
			}

			methodBuilder.append("){\r\n  throw new UnsupportedOperationException();\r\n}");

			CtMethod m = CtNewMethod.make(methodBuilder.toString(), defaultImplCtClass);
			defaultImplCtClass.addMethod(m);
		}

		byte[] bytes = defaultImplCtClass.toBytecode();
		Class<?> invokerClass = SingleClassLoader.loadClass(getClass().getClassLoader(), bytes);

		return invokerClass.getConstructor().newInstance();
	}

}
