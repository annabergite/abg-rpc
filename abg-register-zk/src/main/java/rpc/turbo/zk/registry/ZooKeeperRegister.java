package rpc.abg.zk.registry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import rpc.abg.config.AddressWithWeight;
import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;
import rpc.abg.registry.Register;
import rpc.abg.util.HexUtils;
import rpc.abg.zk.common.ForeverRetryPolicy;

public class ZooKeeperRegister implements Register {
	private static final Log logger = LogFactory.getLog(ZooKeeperRegister.class);

	private CuratorFramework client;
	private ConcurrentMap<String, PathChildrenCache> watcherMap;

	@Override
	public void init(List<HostPort> hostPorts) {
		watcherMap = new ConcurrentHashMap<>();
		String connectString = hostPorts.stream().map(i -> i.toString()).collect(Collectors.joining(","));
		RetryPolicy retryPolicy = new ForeverRetryPolicy(1000, 60 * 1000);
		client = CuratorFrameworkFactory.newClient(connectString, 1000 * 10, 1000 * 3, retryPolicy);
		client.start();
	}

	@Override
	public void register(String group, String app, Protocol protocol, HostPort serverAddress, int serverWeight) {
		Objects.requireNonNull(client, "call init first");

		final String path = "/abg/" + group + "/" + app + "/" + protocol + "/"
				+ HexUtils.toHex(serverAddress.toString().getBytes(StandardCharsets.UTF_8));

		byte[] data = new AddressWithWeight(serverAddress, serverWeight).toBytes();

		try {
			if (client.checkExists().forPath(path) != null) {
				if (logger.isInfoEnabled()) {
					logger.info("删除zk已存在节点: " + path + ", " + serverAddress);
				}

				client.delete().forPath(path);
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("zk已存在节点删除失败, " + path + ", " + serverAddress, e);
			}
		}

		try {
			client//
					.create()//
					.creatingParentsIfNeeded()//
					.withMode(CreateMode.EPHEMERAL)//
					.forPath(path, data);

			if (logger.isInfoEnabled()) {
				logger.info("zk注册成功, " + path + ", " + serverAddress + "@" + serverWeight);
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("zk注册失败, " + path + ", " + serverAddress + "@" + serverWeight, e);
			}
		}

		if (!watcherMap.containsKey(path)) {
			addRegisterWatcher(group, app, protocol, serverAddress, serverWeight);
		}

	}

	/**
	 * 当断掉连接又重新连接上时起作用
	 * 
	 * @param group
	 * @param app
	 * @param protocol
	 * @param serverAddress
	 * @param serverWeight
	 */
	private synchronized void addRegisterWatcher(String group, String app, Protocol protocol, HostPort serverAddress,
			int serverWeight) {
		final String path = "/abg/" + group + "/" + app + "/" + protocol + "/"
				+ HexUtils.toHex(serverAddress.toString().getBytes(StandardCharsets.UTF_8));

		if (watcherMap.containsKey(path)) {
			return;
		}

		PathChildrenCache watcher = new PathChildrenCache(client, path, false);

		PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
			private volatile boolean waitForInitializedEvent = true;

			@Override
			public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
				switch (event.getType()) {

				case INITIALIZED:
					waitForInitializedEvent = false;
					break;

				case CONNECTION_RECONNECTED:
					if (waitForInitializedEvent) {
						return;
					}

					if (logger.isInfoEnabled()) {
						logger.info("获得zk连接尝试重新注册, " + path + ", " + serverAddress + "@" + serverWeight);
					}

					ZooKeeperRegister.this.register(group, app, protocol, serverAddress, serverWeight);

					break;

				default:
					break;
				}
			}
		};

		watcher.getListenable().addListener(pathChildrenCacheListener);

		try {
			watcher.start(StartMode.POST_INITIALIZED_EVENT);
			watcherMap.put(path, watcher);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("zk监听失败, " + path, e);
			}
		}
	}

	@Override
	public void unregister(String group, String app, Protocol protocol, HostPort serverAddress) {
		String path = "/abg/" + group + "/" + app + "/" + protocol + "/"
				+ HexUtils.toHex(serverAddress.toString().getBytes(StandardCharsets.UTF_8));

		try {
			PathChildrenCache watcher = watcherMap.remove(path);
			if (watcher != null) {
				watcher.close();
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("warcher关闭失败, " + path + ", " + serverAddress, e);
			}
		}

		try {
			if (client.checkExists().forPath(path) != null) {
				client.delete().forPath(path);
			}

			if (logger.isInfoEnabled()) {
				logger.info("zk注销成功, " + path + ", " + serverAddress);
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("zk注销失败, " + path + ", " + serverAddress, e);
			}
		}
	}

	@Override
	public void close() throws IOException {
		watcherMap.forEach((path, watcher) -> {
			try {
				watcher.close();
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error("warcher关闭失败, " + path);
				}
			}
		});

		watcherMap = null;

		client.close();
		client = null;
	}
}
