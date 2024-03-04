package rpc.abg.invoke;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Streams;

import rpc.abg.annotation.abgService;
import rpc.abg.common.abgConnectService;
import rpc.abg.server.abgConnectServiceServerImpl;
import rpc.abg.util.FastMap;
import rpc.abg.util.concurrent.ConcurrentArrayList;
import rpc.abg.util.concurrent.ConcurrentIntToObjectArrayMap;

/**
 * 服务端invoker工程类
 * 
 * @author Annabergite
 *
 */
public class ServerInvokerFactory {
	private static final Log logger = LogFactory.getLog(ServerInvokerFactory.class);

	private final String group;
	private final String app;

	public final String restPrefix;

	// 低频使用
	private final ConcurrentMap<String, Boolean> classRegisterMap = new ConcurrentHashMap<>();
	// 高频使用
	private final ConcurrentArrayList<JavassistInvoker<?>> invokerMap = new ConcurrentArrayList<>();
	// 高频使用, 非线程安全, 使用CopyOnWrite的方式添加元素
	private volatile FastMap<String, JavassistInvoker<?>> restInvokerMap = new FastMap<>(32, 0.5F);
	// 高频使用
	private final ConcurrentIntToObjectArrayMap<String> serviceMethodNameMap = new ConcurrentIntToObjectArrayMap<>();

	private final AtomicInteger classIdGenerator = new AtomicInteger();
	private final ConcurrentMap<String, Integer> classIdMap = new ConcurrentHashMap<>();

	public ServerInvokerFactory(String group, String app) {
		this.group = group;
		this.app = app;
		this.restPrefix = "/" + group + "/" + app + "/";

		abgConnectService connectService = new abgConnectServiceServerImpl(this);
		register(abgConnectService.class, connectService);
	}

	public ServerInvokerFactory() {
		this(abgService.DEFAULT_GROUP, abgService.DEFAULT_APP);
	}

	/**
	 * 通过服务id获取invoker
	 * 
	 * @param serviceId
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Invoker<T> get(int serviceId) {
		return (Invoker<T>) invokerMap.get(serviceId);
	}

	/**
	 * 通过restPath获取invoker
	 * 
	 * @param restPath
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Invoker<T> get(String restPath) {
		return (Invoker<T>) restInvokerMap.get(restPath);
	}

	/**
	 * 通过服务id获取服务名称
	 * 
	 * @param serviceId
	 * 
	 * @return
	 */
	public String getServiceMethodName(int serviceId) {
		String serviceMethodName = serviceMethodNameMap.get(serviceId);

		if (serviceMethodName != null) {
			return serviceMethodName;
		}

		return serviceMethodNameMap.getOrUpdate(serviceId, () -> {
			Method method = get(serviceId).getMethod();
			return InvokerUtils.getServiceMethodName(group, app, method);
		});
	}

	/**
	 * 注册invoker
	 * 
	 * @param service
	 *            实现类实例，重复调用时不会覆盖，会一直使用首次注册的实例
	 * 
	 * @param clazz
	 *            服务接口
	 * 
	 */
	public synchronized void register(Class<?> clazz, Object service) {
		register(Map.of(clazz, service));
	}

	/**
	 * 注册invoker
	 * 
	 * @param map
	 *            key:服务接口 value:实现类
	 */
	public synchronized void register(Map<Class<?>, Object> map) {
		if (map == null) {
			throw new InvokeException("map is null");
		}

		final AtomicInteger serviceIdCounter = new AtomicInteger(invokerMap.size());

		List<JavassistInvoker<?>> invokers = map//
				.entrySet()//
				.stream()//
				.flatMap(kv -> convertToInvokerStream(kv.getKey(), kv.getValue(), serviceIdCounter))//
				.collect(Collectors.toList());

		if (invokers.isEmpty()) {
			return;
		}

		invokerMap.addAll(invokers);
		putRestInvoker(invokers);
	}

	// copy on write
	private synchronized void putRestInvoker(List<JavassistInvoker<?>> invokers) {
		FastMap<String, JavassistInvoker<?>> map = new FastMap<>(restInvokerMap);

		invokers.forEach(invoker -> {
			String restPath = InvokerUtils.getRestPath(invoker.method);

			if (!Strings.isNullOrEmpty(restPath)) {
				map.put(restPath, invoker);

				if (logger.isInfoEnabled()) {
					logger.info(InvokerUtils.getServiceMethodName(group, app, invoker.method) + " restPath:"
							+ restPrefix + restPath);
				}
			}
		});

		restInvokerMap = map;
	}

	private <T> Stream<JavassistInvoker<T>> convertToInvokerStream(Class<?> clazz, Object service,
			AtomicInteger serviceIdCounter) {

		if (clazz == null) {
			throw new InvokeException("clazz cannot be null");
		}

		if (service == null) {
			throw new InvokeException("service cannot be null");
		}

		if (!clazz.isInstance(service)) {
			throw new InvokeException("clazz is the interface, service is the implemention");
		}

		if (!clazz.isInterface()) {
			throw new InvokeException("the clazz must be interface");
		}

		if (!Modifier.isPublic(clazz.getModifiers())) {
			throw new InvokeException("the clazz must be public");
		}

		abgService classConfig = clazz.getAnnotation(abgService.class);
		if (classConfig != null && classConfig.ignore()) {
			if (logger.isInfoEnabled()) {
				logger.info(clazz + " ignore");
			}

			return Stream.empty();
		}

		if (classRegisterMap.containsKey(InvokerUtils.getServiceClassName(group, app, clazz))) {
			return Stream.empty();
		}

		Method[] allMethods = clazz.getMethods();
		for (Method method : allMethods) {
			if (!CompletableFuture.class.equals(method.getReturnType())) {
				throw new RuntimeException("method return-type must be CompletableFuture, "
						+ InvokerUtils.getServiceMethodName(group, app, method));
			}
		}

		classRegisterMap.put(InvokerUtils.getServiceClassName(group, app, clazz), Boolean.TRUE);
		if (logger.isInfoEnabled()) {
			logger.info("service " + clazz + " register");
		}

		Stream<Method> methodStream = Stream//
				.of(allMethods)//
				.filter(m -> Modifier.isPublic(m.getModifiers()))//
				.filter(m -> !Modifier.isStatic(m.getModifiers()))//
				.peek(m -> {
					if (!CompletableFuture.class.equals(m.getReturnType())) {
						throw new RuntimeException("method return-type must be CompletableFuture, "
								+ InvokerUtils.getServiceMethodName(group, app, m));
					}
				})//
				.filter(m -> {
					abgService methodConfig = m.getAnnotation(abgService.class);

					if (methodConfig != null && methodConfig.ignore()) {
						if (logger.isInfoEnabled()) {
							logger.info(InvokerUtils.getServiceMethodName(group, app, m) + " ignore");
							return false;
						}
					}

					return true;
				});

		// 特殊处理，保证编号正确性
		if (abgConnectService.class.equals(clazz)) {
			methodStream = methodStream.sorted((m0, m1) -> {
				int sort0 = abgConnectService.serviceOrderMap.get(m0.getName());
				int sort1 = abgConnectService.serviceOrderMap.get(m1.getName());

				return sort0 - sort1;
			});
		}

		// 注册classId
		for (Method method : allMethods) {
			registerClassId(method);
		}

		Stream<JavassistInvoker<T>> invokerStream = methodStream//
				.map(m -> {
					int serviceId = serviceIdCounter.getAndIncrement();

					if (logger.isInfoEnabled()) {
						logger.info(InvokerUtils.getServiceMethodName(group, app, m) + " serviceId:" + serviceId);
					}

					return new JavassistInvoker<>(serviceId, service, m.getDeclaringClass(), m);
				});

		return invokerStream;
	}

	/**
	 * 获取已注册的class
	 * 
	 * @return
	 */
	public List<String> getClassRegisterList() {
		return new ArrayList<>(classRegisterMap.keySet());
	}

	/**
	 * 获取已注册的method:serviceId映射
	 * 
	 * @return
	 */
	public Map<String, Integer> getMethodRegisterMap() {
		Map<String, Integer> registerMap = new HashMap<>();

		for (int i = 0; i < invokerMap.size(); i++) {
			JavassistInvoker<?> invoker = invokerMap.get(i);
			Method method = invoker.method;

			registerMap.put(InvokerUtils.getServiceMethodName(group, app, method), i);
		}

		return registerMap;
	}

	/**
	 * 获取已注册的 className:id 映射
	 * 
	 * @return
	 */
	public Map<String, Integer> getClassIdMap() {
		return classIdMap;
	}

	/**
	 * 获取已注册的rest服务列表
	 * 
	 * @return
	 */
	public List<String> getRestRegisterList() {
		return Streams//
				.stream((Iterable<String>) restInvokerMap.keys())//
				.map(path -> restPrefix + path)//
				.collect(Collectors.toList());
	}

	private void registerClassId(Method method) {
		if (method == null) {
			return;
		}

		Type returnType = method.getGenericReturnType();
		registerClassId(returnType);

		Type[] genericParameterTypes = method.getGenericParameterTypes();
		for (int i = 0; i < genericParameterTypes.length; i++) {
			registerClassId(genericParameterTypes[i]);
		}
	}

	private void registerClassId(Type type) {
		if (type == null) {
			return;
		}

		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			Type rawType = parameterizedType.getRawType();
			registerClassId(rawType);

			Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
			for (int i = 0; i < actualTypeArguments.length; i++) {
				registerClassId(actualTypeArguments[i]);
			}
		} else if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;

			if (clazz.isInterface()) {
				return;
			}

			if (clazz.isPrimitive()) {
				return;
			}

			if (clazz.isArray()) {
				return;
			}

			if (clazz.isEnum()) {
				return;
			}

			if (clazz.isAnnotation()) {
				return;
			}

			if (clazz.isAnonymousClass()) {
				return;
			}

			if (clazz.equals(CompletableFuture.class)) {
				return;
			}

			String className = clazz.getName();

			if (className.startsWith("java.lang.")) {
				return;
			}

			if (classIdMap.containsKey(className)) {
				return;
			}

			int classId = classIdMap//
					.computeIfAbsent(className, key -> classIdGenerator.getAndIncrement());

			logger.info("register Serializer.classId " + className + ":" + classId);

			Method[] childMethods = getChildMethods(clazz);
			for (int i = 0; i < childMethods.length; i++) {
				registerClassId(childMethods[i]);
			}
		}
	}

	private Method[] getChildMethods(Class<?> clazz) {
		BeanInfo info;
		try {
			info = Introspector.getBeanInfo(clazz);
		} catch (Throwable t) {
			return new Method[0];
		}

		PropertyDescriptor[] descriptors = info.getPropertyDescriptors();
		if (descriptors.length == 0) {
			return new Method[0];
		}

		Method[] childMethods = new Method[descriptors.length << 1];

		for (int i = 0; i < descriptors.length; i++) {
			PropertyDescriptor descriptor = descriptors[i];
			childMethods[i] = descriptor.getReadMethod();
			childMethods[i << 1] = descriptor.getWriteMethod();
		}

		return childMethods;
	}

	public static void main(String[] args) throws Exception {

	}

}
