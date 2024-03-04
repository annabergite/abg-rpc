package rpc.abg.transport.client;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import rpc.abg.invoke.InvokerUtils;
import rpc.abg.loadbalance.LoadBalance;
import rpc.abg.loadbalance.Weightable;

/**
 * 
 * @author Annabergite
 *
 */
final class MethodRouter {
	private final Method method;
	private final String serviceMethodName;
	private final LoadBalance<Weightable> loadBalance;

	MethodRouter(String serviceMethodName, LoadBalance<Weightable> loadBalance) {
		this.method = InvokerUtils.toMethod(serviceMethodName);
		this.serviceMethodName = serviceMethodName;
		this.loadBalance = loadBalance;
	}

	void setConnectors(Collection<ConnectorContext> connectors) {
		if (connectors == null || connectors.size() == 0) {
			loadBalance.setWeightables(Collections.emptyList());
			return;
		}

		List<Weightable> supported = connectors.stream()//
				.filter(t -> t.isSupport(serviceMethodName))//
				.collect(Collectors.toList());

		loadBalance.setWeightables(supported);
	}

	ConnectorContext selectConnector() {
		return (ConnectorContext) loadBalance.select();
	}

	Method getMethod() {
		return method;
	}

	String getServiceMethodName() {
		return serviceMethodName;
	}
}
