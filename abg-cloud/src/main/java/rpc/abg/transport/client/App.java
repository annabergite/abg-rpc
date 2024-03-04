package rpc.abg.transport.client;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.EventLoopGroup;
import rpc.abg.annotation.AbgService;
import rpc.abg.common.RemoteContext;
import rpc.abg.common.abgConnectService;
import rpc.abg.config.HostPort;
import rpc.abg.config.client.AppConfig;
import rpc.abg.config.server.Protocol;
import rpc.abg.filter.RpcClientFilter;
import rpc.abg.invoke.InvokeException;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.InvokerUtils;
import rpc.abg.loadbalance.LoadBalanceFactory;
import rpc.abg.loadbalance.Weightable;
import rpc.abg.param.MethodParam;
import rpc.abg.remote.RemoteException;
import rpc.abg.util.concurrent.ConcurrentArrayList;
import rpc.abg.util.concurrent.ConcurrentIntToObjectArrayMap;

public class App implements Closeable {
	private static final Log logger = LogFactory.getLog(App.class);

	public static final int MAX_CONNECTOR_SELECT_TIMES = 10;

	private static final long HEARTBEAT_PERIOD = TimeUnit.SECONDS.toMillis(5);
	private static final long RESCUE_PERIOD = TimeUnit.SECONDS.toMillis(5);

	// 并发的做一些建立连接、心跳等后台工作，线程数量用配置的方式更合理一些，但需要用户深入理解这个逻辑，暂时先这样
	private static final ForkJoinPool appForkJoinPool = new ForkJoinPool(64);

	public final String group;
	public final String app;
	private final AppConfig appConfig;
	private final EventLoopGroup eventLoopGroup;
	private final LoadBalanceFactory<Weightable> loadBalanceFactory;
	private final CopyOnWriteArrayList<RpcClientFilter> filters;

	/** 活跃状态的连接, 低频使用 */
	private final ConcurrentHashMap<HostPort, ConnectorContext> activeMap = new ConcurrentHashMap<>();
	/** 僵尸状态的连接, 低频使用 */
	private final ConcurrentHashMap<HostPort, ConnectorContext> zombieMap = new ConcurrentHashMap<>();
	/** methodId -> MethodRouter, 高频使用 */
	private final ConcurrentArrayList<MethodRouter> methodRouterMap = new ConcurrentArrayList<>();
	/** methodString -> methodId, 低频使用 */
	private final ConcurrentHashMap<String, Integer> methodStringToIdMap = new ConcurrentHashMap<>();
	/** methodId -> methodString, 低频使用 */
	private final ConcurrentIntToObjectArrayMap<String> methodIdToServiceMethodNameMap = new ConcurrentIntToObjectArrayMap<>();
	/** class, 低频使用 */
	private final ConcurrentHashMap<String, Boolean> supportClassMap = new ConcurrentHashMap<>();

	/** 抢救线程 */
	private volatile Thread rescueAndHeartbeatJobThread;

	private long lastHeartbeatTime = 0;
	private long lastRescueTime = 0;

	private volatile boolean isCloseing = false;

	static {
		// 自动资源清理
		Runtime.getRuntime()//
				.addShutdownHook(new Thread(() -> appForkJoinPool.shutdownNow(), "appForkJoinPool-shutdown-thread"));
	}

	public App(EventLoopGroup eventLoopGroup, AppConfig appConfig, CopyOnWriteArrayList<RpcClientFilter> filters) {
		this.eventLoopGroup = eventLoopGroup;
		this.appConfig = appConfig;
		this.group = appConfig.getGroup();
		this.app = appConfig.getApp();
		this.loadBalanceFactory = appConfig.getLoadBalanceFactory();
		this.filters = filters;

		if (appConfig.getDiscover() != null) {
			appConfig.getDiscover().addListener(appConfig.getGroup(), appConfig.getApp(), Protocol.RPC,
					serverWithWeight -> {
						if (isCloseing) {
							return;
						}

						if (logger.isInfoEnabled()) {
							logger.info("Discover检测到服务变化: " + serverWithWeight);
						}

						try {
							setConnect(serverWithWeight);
						} catch (Exception e) {
							if (logger.isErrorEnabled()) {
								logger.error("Discover连接出错: " + serverWithWeight, e);
							}
						}
					});
		}
	}

	/**
	 * 建立连接
	 * 
	 * @param serverAddresss
	 * @throws Exception
	 */
	public void setConnect(HostPort... serverAddresss) throws Exception {
		if (serverAddresss == null || serverAddresss.length == 0) {
			return;
		}

		if (isCloseing) {
			return;
		}

		Map<HostPort, Integer> providerWithWeight = Stream//
				.of(serverAddresss)//
				.collect(Collectors.toMap(t -> t, t -> Integer.valueOf(AbgService.DEFAULT_WEIGHT)));

		setConnect(providerWithWeight);
	}

	/**
	 * 建立连接，推荐使用
	 * 
	 * @param serverWithWeight
	 *            服务提供方地址和权重
	 * @throws Exception
	 */
	public void setConnect(Map<HostPort, Integer> serverWithWeight) throws Exception {
		if (serverWithWeight == null || serverWithWeight.size() == 0) {
			return;
		}

		if (isCloseing) {
			return;
		}

		tryStartDemoJob();

		AtomicBoolean changed = new AtomicBoolean(false);

		// 未建立连接的建立连接
		appForkJoinPool.submit(() -> {
			serverWithWeight//
					.entrySet()//
					.stream()//
					.parallel()// 并发的建立连接
					.filter(kv -> !activeMap.containsKey(kv.getKey()))// 过滤掉已连接上的
					.forEach(kv -> {
						if (isCloseing) {
							return;
						}

						changed.set(true);
						setConnect(kv.getKey(), kv.getValue());
					});
		}).get();

		// 删除多余的连接
		appForkJoinPool.submit(() -> {
			activeMap//
					.entrySet()//
					.stream()//
					.parallel()//
					.forEach(kv -> {
						if (isCloseing) {
							return;
						}

						HostPort serverAddress = kv.getKey();

						if (!serverWithWeight.containsKey(serverAddress)) {
							changed.set(true);
							ConnectorContext context = activeMap.remove(serverAddress);

							try {
								context.close();
							} catch (IOException e) {
								if (logger.isWarnEnabled()) {
									logger.warn(serverAddress + "关闭失败", e);
								}
							}
						}
					});
		}).get();

		// 删除多余的连接
		appForkJoinPool.submit(() -> {
			zombieMap//
					.entrySet()//
					.stream()//
					.parallel()//
					.forEach(kv -> {
						if (isCloseing) {
							return;
						}

						changed.set(true);
						HostPort serverAddress = kv.getKey();

						if (!serverWithWeight.containsKey(serverAddress)) {
							ConnectorContext context = zombieMap.remove(serverAddress);

							try {
								context.close();
							} catch (IOException e) {
								if (logger.isWarnEnabled()) {
									logger.warn(serverAddress + "关闭失败", e);
								}
							}
						}
					});
		}).get();

		if (changed.get()) {
			Collection<ConnectorContext> connectors = activeMap.values();

			int length = methodRouterMap.size();
			for (int i = 0; i < length; i++) {// 重置方法路由
				MethodRouter router = methodRouterMap.get(i);
				router.setConnectors(connectors);
			}
		}
	}

	/**
	 * 建立连接
	 * 
	 * @param serverAddress
	 * @param weight
	 *            权重
	 * @throws Exception
	 */
	private synchronized void setConnect(HostPort serverAddress, int weight) {
		if (isCloseing) {
			return;
		}

		ConnectorContext context = new ConnectorContext(eventLoopGroup, appConfig, filters, serverAddress);

		try {
			context.connect();
			context.setWeight(weight);

			List<String> list = loadClass(context);
			for (String clazz : list) {
				supportClassMap.put(clazz, Boolean.TRUE);
			}

			if (logger.isInfoEnabled()) {
				logger.info(group + "#" + app + " " + serverAddress + " support services: " + list);
			}

			Map<String, Integer> methodStringToServiceIdMap = loadServiceId(context);
			context.setServiceMethodNameToServiceIdMap(methodStringToServiceIdMap);

			context.initSerializer();

			addConnect(context);
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn(serverAddress + "连接失败", e);
			}

			try {
				context.close();
			} catch (Exception e2) {
			}
		}
	}

	private synchronized void addConnect(ConnectorContext context) throws Exception {
		if (isCloseing) {
			return;
		}

		if (context == null) {
			return;
		}

		HostPort serverAddress = context.serverAddress;

		activeMap.put(serverAddress, context);
		zombieMap.remove(serverAddress);
	}

	private synchronized void tryStartDemoJob() {
		if (isCloseing) {
			return;
		}

		if (rescueAndHeartbeatJobThread != null) {
			return;
		}

		Thread _rescueAndHeartbeatJobThread = new Thread(() -> {
			while (!isCloseing) {

				if (!isCloseing && !appForkJoinPool.isShutdown()
						&& System.currentTimeMillis() - lastRescueTime > RESCUE_PERIOD) {
					try {
						appForkJoinPool.submit(() -> rescue()).get();
						lastRescueTime = System.currentTimeMillis();
					} catch (InterruptedException e) {
						break;
					} catch (Throwable e) {
						if (logger.isWarnEnabled()) {
							logger.warn(group + "#" + app + " rescue error", e);
						}
					}
				}

				try {
					Thread.sleep(Math.min(HEARTBEAT_PERIOD, RESCUE_PERIOD));
				} catch (Exception e) {
					break;
				}

				if (!isCloseing && !appForkJoinPool.isShutdown()
						&& System.currentTimeMillis() - lastHeartbeatTime > HEARTBEAT_PERIOD) {
					try {
						appForkJoinPool.submit(() -> heartbeat()).get();
						lastHeartbeatTime = System.currentTimeMillis();
					} catch (InterruptedException e) {
						break;
					} catch (ExecutionException e) {
						if (logger.isWarnEnabled()) {
							logger.warn(group + "#" + app + " heartbeat error", e);
						}
					}
				}
			}
		}, "app-heartbeat-and-rescue-thread-#" + group + "#" + app);
		_rescueAndHeartbeatJobThread.setDaemon(true);
		_rescueAndHeartbeatJobThread.start();
		this.rescueAndHeartbeatJobThread = _rescueAndHeartbeatJobThread;
	}

	public boolean isSupport(Class<?> clazz) {
		return supportClassMap.containsKey(InvokerUtils.getServiceClassName(group, app, clazz));
	}

	/**
	 * 获取methodId
	 * 
	 * @param method
	 * @return
	 */
	public Integer getMethodId(Method method) {
		return getMethodId(InvokerUtils.getServiceMethodName(group, app, method));
	}

	/**
	 * 获取methodId
	 * 
	 * @param methodString
	 * @return
	 */
	public Integer getMethodId(String methodString) {
		Integer methodId = methodStringToIdMap.get(methodString);

		if (methodId != null) {
			return methodId;
		}

		synchronized (this) {
			methodId = methodStringToIdMap.get(methodString);

			if (methodId != null) {
				return methodId;
			}

			MethodRouter router = new MethodRouter(methodString, loadBalanceFactory.newLoadBalance());
			router.setConnectors(activeMap.values());

			methodRouterMap.add(router);

			methodId = methodRouterMap.size() - 1;

			methodStringToIdMap.put(methodString, methodId);
			methodIdToServiceMethodNameMap.put(methodId, methodString);
		}

		return methodId;
	}

	/**
	 * 仅用于测试，不要用于正式使用
	 * 
	 * @param method
	 * 
	 * @param timeout
	 *            超时时间，millseconds
	 * 
	 * @param methodParam
	 * 
	 * @return
	 */
	public CompletableFuture<?> execute(Method method, long timeout, MethodParam methodParam) {
		final Integer methodId = getMethodId(method);

		if (methodId == null) {
			throw new InvokeException(group + "#" + app + " " + "找不到对应的服务, " + method);
		}

		return execute(methodId, timeout, methodParam, null);
	}

	/**
	 * 推荐使用
	 * 
	 * @param methodId
	 * 
	 * @param timeout
	 *            超时时间，millseconds
	 * 
	 * @param methodParam
	 * 
	 * @return
	 */
	public CompletableFuture<?> execute(int methodId, long timeout, MethodParam methodParam,
			Invoker<CompletableFuture<?>> failoverInvoker) {

		MethodRouter router;
		if (methodId < 0 || methodId >= methodRouterMap.size() //
				|| (router = methodRouterMap.get(methodId)) == null) {
			String msg = group + "#" + app + " " + "不支持的方法id, " + methodId;
			logger.error(msg);
			return CompletableFuture.failedFuture(new RemoteException(msg));
		}

		if (filters.size() > 0) {
			RemoteContext.setRemoteMethod(router.getMethod());
			RemoteContext.setServiceMethodName(router.getServiceMethodName());
		}

		ConnectorContext connectorContext = router.selectConnector();
		for (int i = 0; i < MAX_CONNECTOR_SELECT_TIMES; i++) {// 设定有限次数的尝试，防止发生死循环
			if (connectorContext == null) {
				break;
			}

			if (connectorContext.isClosed()) {
				// 说明已经在其他地方关闭，实际不存在资源泄露的问题
				try {
					connectorContext.close();
				} catch (Exception e) {
				}

				connectorContext = router.selectConnector();
			} else if (ThreadLocalRandom.current().nextInt(100) < 20 //
					&& connectorContext.isZombie()) {
				kill(connectorContext);
				connectorContext = router.selectConnector();
			} else {
				break;
			}
		}

		if (connectorContext == null) {
			String msg = group + "#" + app + " " + "request error, 无可用连接 ";

			if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}

			if (failoverInvoker == null) {
				return CompletableFuture.failedFuture(new RemoteException(msg, false));
			} else {
				return failoverInvoker.invoke(methodParam);
			}
		}

		int serviceId = getServiceId(connectorContext, methodId);

		if (serviceId < 0) {
			String msg = group + "#" + app + " " + "找不到对应的服务, methodId: " + methodId;

			if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}

			return CompletableFuture.failedFuture(new RemoteException(msg, false));
		}

		return connectorContext.execute(serviceId, timeout, methodParam, failoverInvoker);
	}

	/**
	 * 干掉有问题的连接
	 * 
	 * @param connectorContext
	 */
	private synchronized void kill(ConnectorContext connectorContext) {
		if (connectorContext == null) {
			return;
		}

		HostPort serverAddress = connectorContext.serverAddress;

		if (logger.isWarnEnabled()) {
			logger.warn(group + "#" + app + " " + serverAddress + " is zombie, and will be killed!");
		}

		activeMap.remove(serverAddress);
		zombieMap.put(serverAddress, connectorContext);

		Collection<ConnectorContext> connectors = activeMap.values();

		int length = methodRouterMap.size();
		for (int i = 0; i < length; i++) {
			MethodRouter router = methodRouterMap.get(i);

			if (!connectorContext.isSupport(router.getServiceMethodName())) {
				continue;
			}

			router.setConnectors(connectors);
		}

		if (logger.isWarnEnabled()) {
			logger.warn(group + "#" + app + " " + serverAddress + " is zombie, and have been killed!");
		}
	}

	private void heartbeat() {
		if (isCloseing) {
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(group + "#" + app + " start heartbeat");
		}

		activeMap//
				.entrySet()//
				.stream()//
				.parallel()//
				.forEach(kv -> {
					if (isCloseing) {
						return;
					}

					HostPort serverAddress = kv.getKey();
					ConnectorContext context = kv.getValue();

					if (!context.heartbeat()) {
						activeMap.remove(serverAddress);
						zombieMap.put(serverAddress, context);

						if (logger.isInfoEnabled()) {
							logger.info(group + "#" + app + " " + serverAddress + " is zombie");
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug(group + "#" + app + " " + serverAddress + " is active");
						}
					}
				});
	}

	private void rescue() {
		if (isCloseing) {
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(group + "#" + app + " start rescue zombies");
		}

		if (zombieMap.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug(group + "#" + app + ", there is no zombie");
			}

			return;
		}

		zombieMap//
				.entrySet()//
				.stream()//
				.parallel()//
				.forEach(kv -> {
					if (isCloseing) {
						return;
					}

					HostPort serverAddress = kv.getKey();
					ConnectorContext context = kv.getValue();

					try {
						context.connect();

						List<String> list = loadClass(context);
						for (String clazz : list) {
							supportClassMap.put(clazz, Boolean.TRUE);
						}

						if (logger.isInfoEnabled()) {
							logger.info("rescue: " + group + "#" + app + " " + context.serverAddress
									+ " support services: " + list);
						}

						Map<String, Integer> methodStringToServiceIdMap = loadServiceId(context);

						context.clear();
						context.setServiceMethodNameToServiceIdMap(methodStringToServiceIdMap);

						context.initSerializer();

						zombieMap.remove(serverAddress);
						activeMap.put(serverAddress, context);

						if (logger.isInfoEnabled()) {
							logger.info(group + "#" + app + " " + context.serverAddress + " zombie is rescued");
						}
					} catch (Exception e) {
						if (logger.isWarnEnabled()) {
							logger.warn("rescue: " + group + "#" + app + " " + context.serverAddress
									+ " zombie is also zombie", e);
						}
					}
				});
	}

	/**
	 * 获取远程服务id
	 * 
	 * @param context
	 * @param methodId
	 * @return -1为找不到
	 */
	private int getServiceId(ConnectorContext context, int methodId) {
		int serviceId = context.getServiceId(methodId);

		if (serviceId < 0) {
			String serviceMethodName = methodIdToServiceMethodNameMap.get(methodId);

			if (serviceMethodName == null) {
				String msg = group + "#" + app + " " + "找不到对应的服务, methodId: " + methodId;

				if (logger.isWarnEnabled()) {
					logger.warn(msg);
				}

				return -1;
			}

			context.putServiceId(serviceMethodName, methodId);
			serviceId = context.getServiceId(methodId);

			if (serviceId < 0) {
				String msg = group + "#" + app + " " + "找不到对应的服务, methodId: " + methodId + ", method: "
						+ serviceMethodName;

				if (logger.isWarnEnabled()) {
					logger.warn(msg);
				}

				return -1;
			}
		}

		return serviceId;
	}

	/**
	 * 添加时调用一次就行了
	 * 
	 * @throws Exception
	 */
	private List<String> loadClass(ConnectorContext context) throws Exception {
		int serviceId = abgConnectService.SERVICE_CLASS_REGISTER;
		long timeout = AbgService.DEFAULT_TIME_OUT;
		CompletableFuture<List<String>> future = context.execute(serviceId, timeout);

		return future.get();
	}

	/**
	 * 添加时调用一次就行了
	 * 
	 * @throws Exception
	 */
	private Map<String, Integer> loadServiceId(ConnectorContext context) throws Exception {
		int serviceId = abgConnectService.SERVICE_METHOD_REGISTER;
		long timeout = AbgService.DEFAULT_TIME_OUT;
		CompletableFuture<Map<String, Integer>> future = context.execute(serviceId, timeout);

		return future.get();
	}

	@Override
	public void close() throws IOException {
		if (isCloseing) {
			return;
		}

		isCloseing = true;
		rescueAndHeartbeatJobThread.interrupt();

		activeMap.forEach((key, connectorContext) -> {
			try {
				connectorContext.close();
			} catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn(group + "#" + app + " " + "关闭过程中发生错误", e);
				}
			}
		});

		activeMap.clear();
		zombieMap.clear();
		methodStringToIdMap.clear();
		methodIdToServiceMethodNameMap.clear();
		supportClassMap.clear();

		if (appConfig.getDiscover() != null) {
			appConfig.getDiscover().close();
		}

	}
}
