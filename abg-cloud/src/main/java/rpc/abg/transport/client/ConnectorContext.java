package rpc.abg.transport.client;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.EventLoopGroup;
import rpc.abg.annotation.AbgService;
import rpc.abg.common.RemoteContext;
import rpc.abg.common.abgConnectService;
import rpc.abg.config.HostPort;
import rpc.abg.config.client.AppConfig;
import rpc.abg.filter.RpcClientFilter;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.InvokerUtils;
import rpc.abg.loadbalance.Weightable;
import rpc.abg.param.EmptyMethodParam;
import rpc.abg.param.MethodParam;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.protocol.ResponseStatus;
import rpc.abg.recycle.RecycleUtils;
import rpc.abg.remote.RemoteException;
import rpc.abg.serialization.Serializer;
import rpc.abg.serialization.SerializerFactory;
import rpc.abg.transport.client.future.RequestWithFuture;
import rpc.abg.util.SystemClock;
import rpc.abg.util.concurrent.AtomicMuiltInteger;
import rpc.abg.util.concurrent.ConcurrentIntToIntArrayMap;
import rpc.abg.util.concurrent.ConcurrentIntegerSequencer;

final class ConnectorContext implements Weightable, Closeable {
	private static final Log logger = LogFactory.getLog(ConnectorContext.class);

	private final AppConfig appConfig;
	public final HostPort serverAddress;

	private final int connectCount;
	private final NettyClientConnector connector;
	private final ConcurrentIntegerSequencer sequencer = new ConcurrentIntegerSequencer(0, true);
	private final Semaphore requestWaitSemaphore;
	private final AtomicMuiltInteger errorCounter;
	private final int globalTimeout;
	private final CopyOnWriteArrayList<RpcClientFilter> filters;
	private final Serializer serializer;

	private final Method heartbeatMethod;
	private final String heartbeatServiceMethodName;

	private final ConcurrentIntToIntArrayMap methodIdToServiceIdMap = new ConcurrentIntToIntArrayMap();
	private volatile Map<String, Integer> serviceMethodNameToServiceIdMap;
	private volatile int weight;
	private volatile boolean isClosed = false;

	ConnectorContext(EventLoopGroup eventLoopGroup, AppConfig appConfig, CopyOnWriteArrayList<RpcClientFilter> filters,
			HostPort serverAddress) {
		this.appConfig = appConfig;
		this.connectCount = appConfig.getConnectPerServer();
		this.serializer = SerializerFactory.createSerializer(appConfig.getSerializer());

		this.connector = new NettyClientConnector(//
				eventLoopGroup, //
				serializer, //
				serverAddress, //
				connectCount);

		this.serverAddress = serverAddress;

		this.errorCounter = new AtomicMuiltInteger(connectCount);

		if (appConfig.getMaxRequestWait() < 1) {
			this.requestWaitSemaphore = null;
		} else {
			this.requestWaitSemaphore = new Semaphore(appConfig.getMaxRequestWait());
		}

		this.globalTimeout = appConfig.getGlobalTimeout();

		this.filters = filters;

		try {
			heartbeatMethod = abgConnectService.class.getDeclaredMethod("heartbeat");
			heartbeatServiceMethodName = InvokerUtils.getServiceMethodName(appConfig.getGroup(), appConfig.getApp(),
					heartbeatMethod);
		} catch (Exception e) {
			throw new RemoteException("error on init", e);
		}
	}

	boolean isSupport(String serviceMethodName) {
		if (serviceMethodNameToServiceIdMap == null) {
			return false;
		}

		return serviceMethodNameToServiceIdMap.containsKey(serviceMethodName);
	}

	void initSerializer() throws Exception {
		if (!serializer.isSupportedClassId()) {
			return;
		}

		int serviceId = abgConnectService.SERVICE_CLASS_ID_REGISTER;
		long timeout = AbgService.DEFAULT_TIME_OUT;

		CompletableFuture<Map<String, Integer>> future = execute(serviceId, timeout);
		Map<String, Integer> classIds = future.get();

		Map<Class<?>, Integer> classIdMap = new HashMap<>();
		classIds.forEach((className, id) -> {
			try {
				classIdMap.put(Class.forName(className), id);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		serializer.setClassIds(classIdMap);

		logger.info(serverAddress + " register Serializer.classIds: " + classIdMap);
	}

	boolean heartbeat() {

		return IntStream//
				.range(0, connector.connectCount())//
				.mapToObj(index -> {

					int requestId = sequencer.next();
					Request request = new Request();
					request.setServiceId(abgConnectService.SERVICE_HEARTBEAT);
					request.setRequestId(requestId);

					CompletableFuture<Response> future = new CompletableFuture<>();

					try {
						if (requestWaitSemaphore != null) {
							requestWaitSemaphore.acquire();
						}

						boolean allowSend = doRequestFilter(request, heartbeatMethod, heartbeatServiceMethodName);
						if (allowSend) {
							long expireTime = SystemClock.fast().mills() + AbgService.DEFAULT_TIME_OUT;
							connector.send(index, new RequestWithFuture(request, future, expireTime));
						} else {
							future.completeExceptionally(
									new RemoteException(RpcClientFilter.CLIENT_FILTER_DENY, false));
						}
					} catch (Exception e) {
						future.completeExceptionally(e);
					}

					CompletableFuture<Boolean> result = handleResult(request, future);

					return result;
				})//
				.allMatch(future -> {
					try {
						return future.join();
					} catch (Throwable e) {
						if (logger.isWarnEnabled()) {
							logger.warn(serverAddress + " heartbeat error", e);
						}

						return false;
					}
				});
	}

	/**
	 * 远程调用，无参，无失败回退
	 * 
	 * @param serviceId
	 *            远程serviceId
	 * @param timeout
	 *            millseconds
	 * @return
	 */
	<T> CompletableFuture<T> execute(int serviceId, long timeout) {
		return execute(serviceId, timeout, null, null);
	}

	/**
	 * 远程调用
	 * 
	 * @param serviceId
	 *            远程serviceId
	 * @param timeout
	 *            millseconds
	 * @param methodParam
	 *            方法参数对象，无参类型为null
	 * @param failoverInvoker
	 *            失败回退
	 * @return
	 */
	<T> CompletableFuture<T> execute(int serviceId, long timeout, MethodParam methodParam,
			Invoker<CompletableFuture<?>> failoverInvoker) {

		if (isClosed) {
			throw new RemoteException("已关闭的连接!");
		}

		int requestId = sequencer.next();

		for (int i = 0; i < connectCount; i++) {// 最多循环一遍
			if (isZombie(channelIndex(requestId))) {
				requestId = sequencer.next();
				continue;
			}

			break;
		}

		Request request = new Request();
		request.setServiceId(serviceId);
		request.setRequestId(requestId);

		if (methodParam instanceof EmptyMethodParam) {
			request.setMethodParam(null);
		} else {
			request.setMethodParam(methodParam);
		}

		if (globalTimeout > 0) {
			timeout = globalTimeout;
		}

		CompletableFuture<Response> future = new CompletableFuture<>();

		try {
			if (requestWaitSemaphore != null) {
				requestWaitSemaphore.acquire();
			}

			boolean allowSend = doRequestFilter(request);
			if (allowSend) {
				long expireTime = SystemClock.fast().mills() + timeout;

				connector.send(//
						channelIndex(request), //
						new RequestWithFuture(request, future, expireTime));
			} else {
				future.completeExceptionally(new RemoteException(RpcClientFilter.CLIENT_FILTER_DENY, false));
			}
		} catch (Exception e) {
			future.completeExceptionally(e);
		}

		if (failoverInvoker == null) {
			return handleResult(request, future);
		} else {
			return handleResult(request, future, failoverInvoker, methodParam);
		}
	}

	private int channelIndex(int requestId) {
		return requestId % connectCount;
	}

	private int channelIndex(Request request) {
		return channelIndex(request.getRequestId());
	}

	/**
	 * 处理返回值，无失败回退
	 * 
	 * @param request
	 * @param future
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> CompletableFuture<T> handleResult(final Request request, final CompletableFuture<Response> future) {

		final Method method;
		final String serviceMethodName;
		if (filters.size() == 0) {
			method = null;
			serviceMethodName = null;
		} else {
			method = RemoteContext.getRemoteMethod();
			serviceMethodName = RemoteContext.getServiceMethodName();
		}

		return future.handle((response, throwable) -> {
			if (requestWaitSemaphore != null) {
				requestWaitSemaphore.release();
			}

			boolean error = false;
			if (throwable != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("request error, requestId: " + request.getRequestId(), throwable);
				}

				error = true;
			}

			if (!error && response == null) {
				String msg = "request error, requestId: " + request.getRequestId();
				if (logger.isWarnEnabled()) {
					logger.warn(msg);
				}

				error = true;
			}

			if (!error && response.getStatusCode() != ResponseStatus.OK) {
				String msg = " status code is" + response.getStatusCode() + " reason is " + response.getResult();

				if (logger.isWarnEnabled()) {
					logger.warn(msg);
				}

				error = true;
			}

			doResponseFilter(request, response, method, serviceMethodName, throwable);

			int channelIndex = channelIndex(request);
			if (error) {
				errorCounter.incrementAndGet(channelIndex);

				return null;
			} else {
				errorCounter.reset(channelIndex);

				T result = (T) response.getResult();
				RecycleUtils.release(response);

				return result;
			}

		});
	}

	/**
	 * 处理返回值，带失败回退
	 * 
	 * @param request
	 * @param future
	 * @param failoverInvoker
	 * @param methodParam
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> CompletableFuture<T> handleResult(//
			final Request request, //
			final CompletableFuture<Response> future, //
			final Invoker<CompletableFuture<?>> failoverInvoker, //
			final MethodParam methodParam) {

		final Method method;
		final String serviceMethodName;
		if (filters.size() == 0) {
			method = null;
			serviceMethodName = null;
		} else {
			method = RemoteContext.getRemoteMethod();
			serviceMethodName = RemoteContext.getServiceMethodName();
		}

		CompletableFuture<T> futureWithFailover = new CompletableFuture<>();

		future.whenComplete((response, throwable) -> {
			if (requestWaitSemaphore != null) {
				requestWaitSemaphore.release();
			}

			boolean error = false;
			if (throwable != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("request error, requestId: " + request.getRequestId(), throwable);
				}

				error = true;
			}

			if (!error && response == null) {
				if (logger.isWarnEnabled()) {
					logger.warn("request error, requestId: " + request.getRequestId());
				}

				error = true;
			}

			if (!error && response.getStatusCode() != ResponseStatus.OK) {
				String msg = " status code is" + response.getStatusCode() + " reason is " + response.getResult();

				if (logger.isWarnEnabled()) {
					logger.warn("request error, requestId: " + request.getRequestId() + msg);
				}

				error = true;
			}

			doResponseFilter(request, response, method, serviceMethodName, throwable);

			int channelIndex = channelIndex(request);
			if (error) {
				if (logger.isInfoEnabled()) {
					logger.info("远程调用发生错误，使用本地回退方法执行");
				}

				errorCounter.incrementAndGet(channelIndex);

				failoverInvoker.invoke(methodParam).whenComplete((r, t) -> {
					if (t != null) {
						futureWithFailover.completeExceptionally(t);
					} else {
						futureWithFailover.complete((T) r);
					}
				});
			} else {
				errorCounter.reset(channelIndex);

				T result = (T) response.getResult();
				RecycleUtils.release(response);

				futureWithFailover.complete(result);
			}
		});

		return futureWithFailover;
	}

	private boolean doRequestFilter(Request request) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return true;
		}

		RemoteContext.setServerAddress(connector.serverAddress);
		RemoteContext.setClientAddress(connector.clientAddress);
		// App中赋值 RemoteContext.setRemoteMethod(method);
		// RemoteContext.setServiceMethodName(serviceMethodName);

		for (int i = 0; i < filterLength; i++) {
			RpcClientFilter filter = filters.get(i);
			if (!filter.onSend(request)) {
				return false;
			}
		}

		return true;
	}

	private boolean doRequestFilter(Request request, Method method, String serviceMethodName) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return true;
		}

		RemoteContext.setServerAddress(connector.serverAddress);
		RemoteContext.setClientAddress(connector.clientAddress);
		RemoteContext.setRemoteMethod(method);
		RemoteContext.setServiceMethodName(serviceMethodName);

		for (int i = 0; i < filterLength; i++) {
			RpcClientFilter filter = filters.get(i);
			if (!filter.onSend(request)) {
				return false;
			}
		}

		return true;
	}

	private void doResponseFilter(Request request, Response response, Method method, String serviceMethodName,
			Throwable throwable) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return;
		}

		RemoteContext.setServerAddress(connector.serverAddress);
		RemoteContext.setClientAddress(connector.clientAddress);
		RemoteContext.setRemoteMethod(method);
		RemoteContext.setServiceMethodName(serviceMethodName);

		if (response.getStatusCode() == ResponseStatus.OK) {
			for (int i = 0; i < filterLength; i++) {
				RpcClientFilter filter = filters.get(i);
				filter.onRecive(request, response);
			}
		} else {
			for (int i = 0; i < filterLength; i++) {
				RpcClientFilter filter = filters.get(i);
				filter.onError(request, response, throwable);
			}
		}
	}

	int getServiceId(int methodId) {
		return methodIdToServiceIdMap.get(methodId);
	}

	void putServiceId(String serviceMethodName, int methodId) {
		Integer serviceId = serviceMethodNameToServiceIdMap.get(serviceMethodName);
		if (serviceId == null) {
			return;
		}

		methodIdToServiceIdMap.put(methodId, serviceId);
	}

	void clear() {
		methodIdToServiceIdMap.clear();
	}

	public void setServiceMethodNameToServiceIdMap(Map<String, Integer> serviceMethodNameToServiceIdMap) {
		this.serviceMethodNameToServiceIdMap = serviceMethodNameToServiceIdMap;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	boolean isZombie() {

		int sum = 0;
		boolean allZombie = true;

		for (int i = 0; i < connectCount; i++) {
			int error = errorCounter.get(i);
			sum += error;

			if (error < appConfig.getConnectErrorThreshold()) {
				allZombie = false;
			}
		}

		return sum >= appConfig.getServerErrorThreshold() || allZombie;
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	private boolean isZombie(int index) {
		return errorCounter.get(index) >= appConfig.getConnectErrorThreshold();
	}

	void connect() throws InterruptedException {
		connector.connect();
		errorCounter.resetAll();
	}

	@Override
	public int weight() {
		return weight;
	}

	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public void close() throws IOException {
		if (isClosed) {
			return;
		}

		isClosed = true;
		connector.close();
	}

}
