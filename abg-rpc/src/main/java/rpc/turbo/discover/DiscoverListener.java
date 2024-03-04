package rpc.abg.discover;

import java.util.Map;

import rpc.abg.config.HostPort;

@FunctionalInterface
public interface DiscoverListener {
	/**
	 * 当服务发生变化时调用
	 * 
	 * @param serverWithWeight
	 */
	void onChange(Map<HostPort, Integer> serverWithWeight);
}
