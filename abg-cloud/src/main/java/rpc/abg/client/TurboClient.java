package rpc.abg.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.EventLoopGroup;
import rpc.abg.common.EventLoopGroupHolder;
import rpc.abg.config.HostPort;
import rpc.abg.config.client.AppConfig;
import rpc.abg.config.client.ClientConfig;
import rpc.abg.filter.RpcClientFilter;
import rpc.abg.invoke.FailoverInvokerFactory;
import rpc.abg.invoke.InvokerUtils;
import rpc.abg.remote.RemoteException;
import rpc.abg.remote.RemoteInterface;
import rpc.abg.remote.RemoteServiceFactory;
import rpc.abg.transport.client.App;

/**
 * 
 * @author Annabergite
 *
 */
public final class abgClient implements Closeable {
	private static final Log logger = LogFactory.getLog(abgClient.class);

	private final FailoverInvokerFactory failoverInvokerFactory = new FailoverInvokerFactory();
	private final RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(failoverInvokerFactory);
	private final ConcurrentHashMap<App, Boolean> appMap = new ConcurrentHashMap<>();
	private final EventLoopGroup eventLoopGroup;
	private final CopyOnWriteArrayList<RpcClientFilter> filters = new CopyOnWriteArrayList<>();

	public abgClient() {
		this(new ClientConfig());
	}

	public abgClient(String resourceName) {
		this(ClientConfig.parse(resourceName));
	}

	public abgClient(ClientConfig clientConfig) {
		eventLoopGroup = EventLoopGroupHolder.get();

		if (clientConfig != null && clientConfig.getAppConfigList() != null) {
			clientConfig.getAppConfigList().forEach(appConfig -> addConnect(appConfig));
		}
	}

	public void addConnect(AppConfig appConfig) {
		Objects.requireNonNull(appConfig, "appConfig");

		try {
			App _app = new App(eventLoopGroup, appConfig, filters);
			appMap.put(_app, Boolean.TRUE);
		} catch (Exception e) {
			throw new RemoteException(e);
		}
	}

	/**
	 * 手动建立连接
	 * 
	 * @param group
	 * @param hostPorts
	 */
	public void addConnect(String group, String app, HostPort... hostPorts) {
		Objects.requireNonNull(group, "group");
		Objects.requireNonNull(app, "app");
		Objects.requireNonNull(hostPorts, "hostPorts");

		try {
			AppConfig appConfig = new AppConfig();
			appConfig.setGroup(group);
			appConfig.setApp(app);

			App _app = new App(eventLoopGroup, appConfig, filters);
			_app.setConnect(hostPorts);
			appMap.put(_app, Boolean.TRUE);
		} catch (Exception e) {
			throw new RemoteException(e);
		}
	}

	/**
	 * 注册一个远程服务
	 * 
	 * @param group
	 * @param app
	 * @param clazz
	 * @param failover
	 */
	public <T> void register(String group, String app, Class<T> clazz, Object failover) {
		T service = remoteServiceFactory.getService(clazz);

		if (service != null) {
			return;
		}

		Objects.requireNonNull(group, "group");
		Objects.requireNonNull(app, "app");

		Optional<App> appOptional = appMap//
				.keySet()//
				.stream()//
				.filter(v -> v.group.equals(group))//
				.filter(v -> v.app.equals(app))//
				.filter(v -> v.isSupport(clazz))//
				.findFirst();

		if (!appOptional.isPresent()) {
			throw new RemoteException(
					"not support this service, " + InvokerUtils.getServiceClassName(group, app, clazz));
		}

		App application = appOptional.get();

		try {
			remoteServiceFactory.register(application, clazz);
			remoteServiceFactory.setFailover(application, clazz, service, failover);
		} catch (Exception e) {
			throw new RemoteException(e);
		}
	}

	/**
	 * 注册一个远程服务
	 * 
	 * @param clazz
	 */
	public <T> void register(Class<T> clazz) {
		Objects.requireNonNull(clazz, "clazz");

		T service = remoteServiceFactory.getService(clazz);

		if (service != null) {
			return;
		}

		Optional<App> appOptional = appMap//
				.keySet()//
				.stream()//
				.filter(v -> v.isSupport(clazz))//
				.findFirst();

		if (!appOptional.isPresent()) {
			throw new RemoteException("not support this service, " + clazz.getName());
		}

		App app = appOptional.get();

		try {
			remoteServiceFactory.register(app, clazz);
		} catch (Exception e) {
			throw new RemoteException(e);
		}
	}

	/**
	 * 设置失败回退方法
	 * 
	 * @param clazz
	 * @param failover
	 */
	public <T> void setFailover(Class<T> clazz, Object failover) {
		Objects.requireNonNull(clazz, "clazz");

		T service = remoteServiceFactory.getService(clazz);

		if (service == null) {
			throw new RemoteException("not register this service, " + clazz.getName());
		}

		try {
			App app = ((RemoteInterface) service).getApp();
			remoteServiceFactory.setFailover(app, clazz, service, failover);
		} catch (Exception e) {
			throw new RemoteException(e);
		}
	}

	/**
	 * 获取一个远程服务，必须先注册 :<br>
	 * {@link #register(Class)} or {@link #register(String, String, Class, Object)}
	 * 
	 * @param clazz
	 * @return
	 */
	public <T> T getService(Class<T> clazz) {
		return remoteServiceFactory.getService(clazz);
	}

	/**
	 * 添加过滤器到最前面，最前面的会被最先执行
	 * 
	 * @param filter
	 */
	public void addFirst(RpcClientFilter filter) {
		filters.add(0, filter);
	}

	/**
	 * 添加过滤器到最后面，最后面的会被最后执行
	 * 
	 * @param filter
	 */
	public void addLast(RpcClientFilter filter) {
		filters.add(filter);
	}

	@Override
	public void close() throws IOException {

		appMap.forEachKey(4, app -> {
			try {
				app.close();
			} catch (IOException e) {
				if (logger.isWarnEnabled()) {
					logger.warn("client close error", e);
				}
			}
		});

		EventLoopGroupHolder.release(eventLoopGroup);

		appMap.clear();

		remoteServiceFactory.close();
	}

}
