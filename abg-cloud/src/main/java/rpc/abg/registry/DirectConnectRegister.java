package rpc.abg.registry;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;

public class DirectConnectRegister implements Register {
	private static final Log logger = LogFactory.getLog(DirectConnectRegister.class);
	private static final AtomicInteger INDEXER = new AtomicInteger(0);

	private final String name = getClass().getSimpleName() + "-" + INDEXER.getAndIncrement();

	@Override
	public void init(List<HostPort> hostPorts) {
		if (logger.isInfoEnabled()) {
			logger.info(name + " 初始化服务注册器:" + hostPorts);
		}
	}

	@Override
	public void register(String group, String app, Protocol protocol, HostPort serverAddress, int servierWeight) {
		if (logger.isInfoEnabled()) {
			logger.info(name + " 注册服务器:" + group + "#" + app + ", protocol:" + protocol + ", serverAddress:"
					+ serverAddress + ", weight:" + servierWeight);
		}
	}

	@Override
	public void unregister(String group, String app, Protocol protocol, HostPort serverAddress) {
		if (logger.isInfoEnabled()) {
			logger.info(name + " 取消注册服务器:" + group + "#" + app + ", protocol:" + protocol + ", serverAddress:"
					+ serverAddress);
		}
	}

	@Override
	public void close() throws IOException {
		
	}

}
