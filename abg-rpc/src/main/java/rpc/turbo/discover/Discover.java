package rpc.abg.discover;

import java.io.Closeable;
import java.util.List;

import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;

/**
 * 服务发现，App级别，每个App一个Discover
 * 
 * @author Annabergite
 *
 */
public interface Discover extends Closeable {
	/**
	 * 注册中心地址
	 */
	void init(List<HostPort> hostPorts);

	/**
	 * 添加一个服务变化回调
	 * 
	 * @param group
	 * @param app
	 * @param protocol
	 * @param listener
	 */
	void addListener(String group, String app, Protocol protocol, DiscoverListener listener);
}
