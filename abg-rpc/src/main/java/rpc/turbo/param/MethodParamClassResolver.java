package rpc.abg.param;

import java.util.Objects;

import rpc.abg.invoke.ServerInvokerFactory;

/**
 * 服务端专用，通过serviceId获取MethodParam.class
 * 
 * @author Annabergite
 *
 */
public class MethodParamClassResolver {
	private final ServerInvokerFactory invokerFactory;

	public MethodParamClassResolver(ServerInvokerFactory invokerFactory) {
		Objects.requireNonNull(invokerFactory, "invokerFactory is null");

		this.invokerFactory = invokerFactory;
	}

	/**
	 * 服务端专用，通过serviceId获取MethodParam.class
	 * 
	 * @param serviceId
	 * @return
	 */
	public Class<? extends MethodParam> getMethodParamClass(int serviceId) {
		return invokerFactory.get(serviceId).getMethodParamClass();
	}
}
