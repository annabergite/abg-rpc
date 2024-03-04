package rpc.abg.remote;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rpc.abg.invoke.Invoker;
import rpc.abg.param.MethodParam;
import rpc.abg.transport.client.App;

/**
 * 提供远程调用的模板方法，仅供内部使用
 * 
 * @author Annabergite
 *
 */
public interface RemoteInterface {
	static final Log logger = LogFactory.getLog(RemoteInterface.class);
	static final RemoteException IGNORED = new RemoteException("this method is ignored", false);

	public App getApp();

	/**
	 * 
	 * @param methodId
	 * @param timeout
	 *            超时时间，millseconds
	 * @param methodParam
	 * @param failoverInvoker
	 * @return
	 */
	default public CompletableFuture<?> $remote_execute(int methodId, long timeout, MethodParam methodParam,
			Invoker<CompletableFuture<?>> failoverInvoker) {
		try {
			return getApp().execute(methodId, timeout, methodParam, failoverInvoker);
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("远程调用发生错误");
			}

			if (failoverInvoker == null) {
				return CompletableFuture.failedFuture(e);
			} else {
				if (logger.isInfoEnabled()) {
					logger.info("远程调用发生错误，使用本地回退方法执行");
				}

				return failoverInvoker.invoke(methodParam);
			}
		}
	}

	default public CompletableFuture<?> $remote_ignore() {
		return CompletableFuture.failedFuture(IGNORED);
	}
}
