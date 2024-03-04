package rpc.abg.registry;

import java.io.Closeable;
import java.util.List;

import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;

/**
 * 服务注册，一个provider可以有多个Register
 * 
 * @author Annabergite
 *
 */
public interface Register extends Closeable {

	/**
	 * 
	 * @param hostPorts
	 *            注册中心地址
	 */
	void init(List<HostPort> hostPorts);

	/**
	 * 注册服务
	 * 
	 * @param group
	 * @param app
	 * @param protocol
	 * @param serverAddress
	 * @param serverWeight
	 *            0-100
	 */
	void register(String group, String app, Protocol protocol, HostPort serverAddress, int serverWeight);

	/**
	 * 取消注册
	 * 
	 * @param group
	 * @param app
	 * @param protocol
	 * @param serverAddress
	 */
	void unregister(String group, String app, Protocol protocol, HostPort serverAddress);
}
