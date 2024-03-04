package rpc.abg.remote;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import rpc.abg.annotation.abgService;
import rpc.abg.config.MethodConfig;
import rpc.abg.invoke.FailoverInvokerFactory;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.InvokerUtils;
import rpc.abg.param.MethodParam;
import rpc.abg.param.MethodParamClassFactory;
import rpc.abg.transport.client.App;

/**
 * 远程服务工厂类
 * 
 * @author Annabergite
 *
 */
public class RemoteServiceFactory implements Closeable {
	private static final Log logger = LogFactory.getLog(RemoteServiceFactory.class);

	private final FailoverInvokerFactory failoverInvokerFactory;
	// 低频使用
	private final ConcurrentHashMap<Class<?>, Object> remoteServiceMap = new ConcurrentHashMap<>();

	public RemoteServiceFactory() {
		this(null);
	}

	public RemoteServiceFactory(FailoverInvokerFactory failoverInvokerFactory) {
		this.failoverInvokerFactory = failoverInvokerFactory;
	}

	@Override
	public void close() throws IOException {
		// 没啥好清理的
		// TODO 动态生成的类无法被释放，应该要有独立的类加载器
	}

	/**
	 * 通过服务接口获取远程代理实现类，结果缓存起来使用，不要每次调用
	 * 
	 * @param clazz
	 *            服务接口
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getService(Class<T> clazz) {
		return (T) remoteServiceMap.get(clazz);
	}

	/**
	 * 注册一个远程代理，timeout:DEFAULT_TIME_OUT
	 * 
	 * @param app
	 *            连接组
	 * 
	 * @param clazz
	 *            服务接口
	 * 
	 * 
	 * @throws Exception
	 */
	public synchronized void register(App app, Class<?> clazz) throws Exception {
		if (app == null) {
			throw new RuntimeException("app must not be null");
		}

		if (clazz == null) {
			throw new RuntimeException("clazz must not be null");
		}

		if (getService(clazz) != null) {
			return;
		}

		Method[] allMethods = clazz.getMethods();
		Collection<MethodConfig> configs = Stream//
				.of(allMethods)//
				.filter(m -> Modifier.isPublic(m.getModifiers()))//
				.filter(m -> !Modifier.isStatic(m.getModifiers()))//
				.peek(m -> {
					if (!CompletableFuture.class.equals(m.getReturnType())) {
						throw new RuntimeException("method return-type must be CompletableFuture, "
								+ InvokerUtils.getServiceMethodName("", "", m));
					}
				})//
				.map(method -> new MethodConfig(method))//
				.collect(Collectors.toList());

		register(app, clazz, configs);
	}

	/**
	 * 注册一个远程代理
	 * 
	 * @param app
	 *            连接组
	 * 
	 * @param clazz
	 *            服务接口
	 * 
	 * @param configs
	 *            远程方法配置
	 * 
	 * @throws Exception
	 */
	public synchronized void register(App app, Class<?> clazz, Collection<MethodConfig> configs) throws Exception {

		if (app == null) {
			throw new RuntimeException("app must not be null");
		}

		if (clazz == null) {
			throw new RuntimeException("clazz must not be null");
		}

		if (!app.isSupport(clazz)) {
			throw new RuntimeException("the remote service not support the service " + clazz.getName());
		}

		if (configs == null || configs.isEmpty()) {
			throw new RuntimeException("configs must not be empty");
		}

		if (getService(clazz) != null) {
			return;
		}

		abgService classConfig = clazz.getAnnotation(abgService.class);
		if (classConfig != null && classConfig.ignore()) {
			if (logger.isInfoEnabled()) {
				logger.info(clazz + " service ignore");
			}

			return;
		}

		if (logger.isInfoEnabled()) {
			logger.info("service " + clazz.getName() + " register");
		}

		Object service = generateRemoteObject(app, clazz, configs);
		remoteServiceMap.put(clazz, service);

		if (failoverInvokerFactory == null) {
			return;
		}
	}

	public void setFailover(App app, Class<?> clazz, Object service, Object failover) {
		// 设置failoverInvoker
		failoverInvokerFactory.register(clazz, failover);
		Method[] allMethods = clazz.getMethods();

		for (Method method : allMethods) {
			if (!Modifier.isPublic(method.getModifiers())) {
				continue;
			}

			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			Integer methodId = app.getMethodId(method);

			if (methodId == null) {
				continue;
			}

			Invoker<CompletableFuture<?>> failoverInvoker = failoverInvokerFactory.get(method);

			setFailoverInvoker(app, method, service, failoverInvoker);
		}
	}

	// 私有方法开始，下面的方法都不用关注

	private String getFailoverInvokerFieldName(App app, Method method) {
		Integer methodId = app.getMethodId(method);

		if (methodId == null) {
			String msg = "cannot get the methodId, " + InvokerUtils.getServiceMethodName(app.group, app.app, method);
			throw new RuntimeException(msg);
		}

		return "$failoverInvoker_" + methodId;
	}

	/**
	 * 低频使用，不需要考虑性能问题
	 * 
	 * @param app
	 * @param method
	 * @param service
	 * @param failoverInvoker
	 */
	private void setFailoverInvoker(App app, Method method, Object service,
			Invoker<CompletableFuture<?>> failoverInvoker) {
		try {
			String failoverFieldName = getFailoverInvokerFieldName(app, method);
			Field failoverField = service.getClass().getDeclaredField(failoverFieldName);
			failoverField.setAccessible(true);
			failoverField.set(service, failoverInvoker);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Object generateRemoteObject(App app, Class<?> clazz, Collection<MethodConfig> configs) throws Exception {

		if (app == null) {
			throw new RuntimeException("app must not be null");
		}

		if (clazz == null) {
			throw new RuntimeException("clazz must not be null");
		}

		if (configs == null || configs.isEmpty()) {
			throw new RuntimeException("configs must not be empty");
		}

		for (MethodConfig config : configs) {
			if (config == null) {
				throw new RuntimeException("config must not be null");
			}

			if (config.method == null) {
				throw new RuntimeException("config.method must not be null");
			}

			if (config.timeout < 1) {
				throw new RuntimeException("config.timeout must > 0");
			}

			if (config.timeout > 5 * 60 * 1000) {
				throw new RuntimeException("config.timeout must < 5 * 60 * 1000 (5 mintues)");
			}
		}

		Method[] allMethods = clazz.getMethods();
		List<Method> allPublicMethods = Stream//
				.of(allMethods)//
				.filter(m -> Modifier.isPublic(m.getModifiers()))//
				.filter(m -> !Modifier.isStatic(m.getModifiers()))//
				.peek(m -> {
					if (!CompletableFuture.class.equals(m.getReturnType())) {
						throw new RuntimeException("method return-type must be CompletableFuture, "
								+ InvokerUtils.getServiceMethodName("", "", m));
					}
				})//
				.collect(Collectors.toList());

		if (configs.size() != allPublicMethods.size()) {
			throw new RuntimeException("configs must contains all the interface's methods");
		}

		for (Method method : allMethods) {
			if (!CompletableFuture.class.equals(method.getReturnType())) {
				throw new RuntimeException("method return-type must be CompletableFuture, " + method);
			}

			if (!configs.stream().anyMatch(config -> config.method.equals(method))) {
				throw new RuntimeException("configs must contains the method: " + method);
			}
		}

		final String remoteClassName = clazz.getName() + "_RemoteService_"//
				+ UUID.randomUUID().toString().replace("-", "");

		// 创建类
		ClassPool pool = ClassPool.getDefault();
		CtClass remoteCtClass = pool.makeClass(remoteClassName);

		CtClass[] interfaces = { pool.getCtClass(clazz.getName()), pool.getCtClass(RemoteInterface.class.getName()) };
		remoteCtClass.setInterfaces(interfaces);

		// 添加私有成员app
		CtField appField = new CtField(pool.get(App.class.getName()), "app", remoteCtClass);
		appField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
		remoteCtClass.addField(appField);

		// 添加get方法
		remoteCtClass.addMethod(CtNewMethod.getter("getApp", appField));

		// 添加有参的构造函数
		CtConstructor constructor1 = new CtConstructor(new CtClass[] { pool.get(App.class.getName()) }, remoteCtClass);
		constructor1.setBody("{$0.app = $1;}");
		remoteCtClass.addConstructor(constructor1);

		for (MethodConfig config : configs) {
			Method method = config.method;
			Class<? extends MethodParam> methodParamClass = MethodParamClassFactory.createClass(method);
			long timeout = config.timeout;

			// 添加私有成员failoverInvoker
			String failoverFieldName = getFailoverInvokerFieldName(app, method);
			CtField failoverField = new CtField(pool.get(Invoker.class.getName()), failoverFieldName, remoteCtClass);
			appField.setModifiers(Modifier.PRIVATE);
			remoteCtClass.addField(failoverField);

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

			methodBuilder.append("){\r\n");

			methodBuilder.append("  return ");

			if (config.ignore) {
				if (logger.isInfoEnabled()) {
					logger.info(InvokerUtils.getServiceMethodName(app.group, app.app, config.method) + " ignore");
				}

				methodBuilder.append("$remote_ignore()");
			} else {
				if (logger.isInfoEnabled()) {
					logger.info(InvokerUtils.getServiceMethodName(app.group, app.app, config.method) //
							+ " register, config:" + config);
				}

				methodBuilder.append("$remote_execute(");

				methodBuilder.append(app.getMethodId(method));
				methodBuilder.append(", ");
				methodBuilder.append(timeout);
				methodBuilder.append("L, ");
				methodBuilder.append("new ");
				methodBuilder.append(methodParamClass.getName());
				methodBuilder.append("(");

				for (int i = 0; i < parameterTypes.length; i++) {
					methodBuilder.append("param");
					methodBuilder.append(i);

					if (i != parameterTypes.length - 1) {
						methodBuilder.append(",");
					}
				}

				methodBuilder.append("), ");

				methodBuilder.append(failoverFieldName);
				methodBuilder.append(")");
			}

			methodBuilder.append(";\r\n}");

			CtMethod m = CtNewMethod.make(methodBuilder.toString(), remoteCtClass);
			remoteCtClass.addMethod(m);
		}

		Class<?> invokerClass = remoteCtClass.toClass();

		return invokerClass.getConstructor(App.class).newInstance(app);
	}

}
