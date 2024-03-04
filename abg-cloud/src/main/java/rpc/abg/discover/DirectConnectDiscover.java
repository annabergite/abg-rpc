package rpc.abg.discover;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import rpc.abg.annotation.abgService;
import rpc.abg.config.HostPort;
import rpc.abg.config.server.Protocol;

public class DirectConnectDiscover implements Discover {

	private List<HostPort> hostPorts;

	@Override
	public void init(List<HostPort> hostPorts) {
		this.hostPorts = hostPorts;
	}

	@Override
	public void addListener(String group, String app, Protocol protocol, DiscoverListener listener) {
		Map<HostPort, Integer> providerWithWeight = hostPorts.stream()
				.collect(Collectors.toMap(item -> item, item -> abgService.DEFAULT_WEIGHT));

		listener.onChange(providerWithWeight);
	}

	@Override
	public void close() throws IOException {
		System.out.println("DirectConnectDiscover close");
	}

}
