package rpc.abg.server;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.EventLoopGroup;
import rpc.abg.common.EventLoopGroupHolder;
import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;
import rpc.abg.config.server.ServerConfig;
import rpc.abg.filter.RestServerFilter;
import rpc.abg.filter.RpcServerFilter;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.param.MethodParamClassResolver;
import rpc.abg.serialization.Serializer;
import rpc.abg.serialization.SerializerFactory;
import rpc.abg.transport.server.rest.NettyRestServer;
import rpc.abg.transport.server.rpc.NettyRpcServer;

public final class abgServer implements Closeable {
	private static final Log logger = LogFactory.getLog(abgServer.class);

	// 线程数量用配置的方式更合理一些，但需要用户深入理解这个逻辑，暂时先这样吧
	private static final ForkJoinPool serverForkJoinPool = new ForkJoinPool(64);

	private Map<HostPort, Closeable> serverMap = new HashMap<>();
	private Set<Integer> portSet = new HashSet<>();

	private boolean classIdRegistered = false;

	private final Serializer serializer;
	private final ServerInvokerFactory invokerFactory;
	private final EventLoopGroup eventLoopGroup;
	private final ServerConfig serverConfig;
	private final CopyOnWriteArrayList<RpcServerFilter> rpcFilters = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<RestServerFilter> restFilters = new CopyOnWriteArrayList<>();

	private volatile boolean isClosed = false;

	static {
		// 自动资源清理
		Runtime.getRuntime().addShutdownHook(
				new Thread(() -> serverForkJoinPool.shutdownNow(), "serverForkJoinPool-shutdown-thread"));
	}

	/**
	 * 需要执行 {@link #startAndRegisterServer()}
	 * 
	 * @param serverConfig
	 */
	public abgServer(ServerConfig serverConfig, ServerInvokerFactory invokerFactory) {
		Objects.requireNonNull(serverConfig, "serverConfig");
		Objects.requireNonNull(invokerFactory, "invokerFactory");
		this.serverConfig = serverConfig;
		this.invokerFactory = invokerFactory;
		this.eventLoopGroup = EventLoopGroupHolder.get();

		this.serializer = SerializerFactory.createSerializer(serverConfig.getSerializer());

		MethodParamClassResolver classResolver = new MethodParamClassResolver(invokerFactory);
		this.serializer.setClassResolver(classResolver);
	}

	/**
	 * 需要手工startServer
	 * 
	 * @param group
	 * @param app
	 */
	public abgServer(String group, String app) {
		this(new ServerConfig(group, app), new ServerInvokerFactory(group, app));
	}

	public void addFirst(RpcServerFilter filter) {
		rpcFilters.add(0, filter);
	}

	public void addLast(RpcServerFilter filter) {
		rpcFilters.add(filter);
	}

	public void addFirst(RestServerFilter filter) {
		restFilters.add(0, filter);
	}

	public void addLast(RestServerFilter filter) {
		restFilters.add(filter);
	}

	/**
	 * 根据config自动启动并注册服务器
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void startAndRegisterServer() throws InterruptedException, ExecutionException {
		if (serverConfig.getRegisters() == null || serverConfig.getRegisters().isEmpty()) {
			return;
		}

		serverForkJoinPool.submit(() -> {
			serverConfig.getRegisters().parallelStream().forEach(registerConfig -> {
				try {
					if (!serverMap.containsKey(registerConfig.getServerAddress())) {
						Closeable server = startServer(registerConfig.getProtocol(), registerConfig.getServerAddress());

						portSet.add(registerConfig.getServerAddress().port);
						serverMap.put(registerConfig.getServerAddress(), server);
					}

					if (registerConfig.getRegister() != null) {
						registerConfig.getRegister().register(//
								serverConfig.getGroup(), //
								serverConfig.getApp(), //
								registerConfig.getProtocol(), //
								registerConfig.getServerAddress(), //
								registerConfig.getServerWeight());
					}

				} catch (Exception e) {
					if (logger.isErrorEnabled()) {
						logger.error("启动失败，" + registerConfig);
						return;
					}
				}
			});
		}).get();
	}

	public Closeable startServer(Protocol protocol, HostPort hostPort) throws InterruptedException {

		switch (protocol) {
		case RPC:
			return startRpcServer(hostPort);

		case REST:
			return startRestServer(hostPort);

		default:
			throw new UnsupportedOperationException();
		}
	}

	public NettyRpcServer startRpcServer(HostPort hostPort) throws InterruptedException {
		if (!classIdRegistered && serializer.isSupportedClassId()) {
			classIdRegistered = true;

			Map<String, Integer> classIdMap = invokerFactory.getClassIdMap();
			Map<Class<?>, Integer> classIds = new HashMap<>();

			classIdMap.forEach((className, id) -> {
				try {
					classIds.put(Class.forName(className), id);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			serializer.setClassIds(classIds);

			logger.info("register Serializer.classIds: " + classIdMap);
		}

		NettyRpcServer nettyRpcServer = new NettyRpcServer(eventLoopGroup, invokerFactory, serializer, rpcFilters,
				hostPort);
		nettyRpcServer.start();

		return nettyRpcServer;
	}

	public NettyRestServer startRestServer(HostPort hostPort) throws InterruptedException {
		NettyRestServer nettyRestServer = new NettyRestServer(eventLoopGroup, invokerFactory,
				serverConfig.getJsonMapper(), restFilters, hostPort);
		nettyRestServer.start();
		return nettyRestServer;
	}

	public void waitUntilShutdown() {
		while (!eventLoopGroup.isShutdown() && !eventLoopGroup.isShuttingDown()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				try {
					close();
				} catch (IOException e1) {
				}

				break;
			}
		}
	}

	/**
	 * 手动注册一个服务
	 * 
	 * @param map
	 */
	public void registerService(Map<Class<?>, Object> map) {
		invokerFactory.register(map);
	}

	private void unRegisterServer() throws InterruptedException, ExecutionException {
		if (serverConfig.getRegisters() == null || serverConfig.getRegisters().isEmpty()) {
			return;
		}

		serverForkJoinPool.submit(() -> {
			serverConfig.getRegisters().parallelStream().forEach(registerConfig -> {
				try {
					if (registerConfig.getRegister() != null) {
						registerConfig.getRegister().unregister(//
								serverConfig.getGroup(), //
								serverConfig.getApp(), //
								registerConfig.getProtocol(), //
								registerConfig.getServerAddress());
					}
				} catch (Exception e) {
					if (logger.isErrorEnabled()) {
						logger.error("注销失败，" + registerConfig);
						return;
					}
				}
			});

			serverConfig.getRegisters().parallelStream().forEach(registerConfig -> {
				try {
					if (registerConfig.getRegister() != null) {
						registerConfig.getRegister().close();
					}
				} catch (Exception e) {
					if (logger.isErrorEnabled()) {
						logger.error("关闭失败，" + registerConfig);
						return;
					}
				}
			});
		}).get();
	}

	@Override
	public void close() throws IOException {
		if (isClosed) {
			return;
		}

		isClosed = true;

		try {
			unRegisterServer();
		} catch (Exception e) {
			// 不会出错的
		}

		serverMap.forEach((hostPort, server) -> {
			try {
				server.close();

				if (logger.isInfoEnabled()) {
					logger.info("成功关闭服务器: " + server.getClass().getName() + " " + hostPort);
				}
			} catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("关闭服务器出错: " + hostPort, e);
				}
			}
		});

		EventLoopGroupHolder.release(eventLoopGroup);
	}
}
