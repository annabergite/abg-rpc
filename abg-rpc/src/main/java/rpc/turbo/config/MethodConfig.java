package rpc.abg.config;

import java.lang.reflect.Method;

import rpc.abg.annotation.abgService;
import rpc.abg.invoke.InvokerUtils;

/**
 * 服务端、客户端通用
 * 
 * @author Annabergite
 *
 */
public class MethodConfig {
	/** 服务方法 */
	public final Method method;
	/** 版本 */
	public final String version;
	/** millseconds */
	public final long timeout;
	/** 忽略 */
	public final boolean ignore;
	/** rest路径 */
	public final String rest;

	/**
	 * @param method
	 *            服务方法
	 */
	public MethodConfig(Method method) {
		this.method = method;

		this.version = version(method);
		this.timeout = timeout(method);
		this.ignore = ignore(method);
		this.rest = rest(method);
	}

	public MethodConfig(Method method, String version, long timeout, boolean ignore, String rest) {
		this.method = method;
		this.version = version;
		this.timeout = timeout;
		this.ignore = ignore;
		this.rest = rest;
	}

	private String version(Method method) {
		String version = abgService.DEFAULT_VERSION;

		abgService config = method.getDeclaringClass().getAnnotation(abgService.class);
		if (config == null) {
			config = method.getAnnotation(abgService.class);
		}

		if (config != null) {
			version = config.version();
		}

		int delimterIndex = version.indexOf('.');
		if (delimterIndex > 0) {
			version = version.substring(0, delimterIndex);
		}

		return version;
	}

	private long timeout(Method method) {
		long timeout = abgService.DEFAULT_TIME_OUT;

		abgService config = method.getDeclaringClass().getAnnotation(abgService.class);
		if (config == null) {
			config = method.getAnnotation(abgService.class);
		}

		if (config != null) {
			timeout = config.timeout();
		}

		if (timeout < 1) {
			timeout = abgService.DEFAULT_TIME_OUT;
		}

		return timeout;
	}

	private boolean ignore(Method method) {
		boolean ignore = abgService.DEFAULT_IGNORE;

		abgService config = method.getDeclaringClass().getAnnotation(abgService.class);

		if (config != null) {
			ignore = config.ignore();
		}

		if (!ignore) {
			config = method.getAnnotation(abgService.class);

			if (config != null) {
				ignore = config.ignore();
			}
		}

		return ignore;
	}

	private String rest(Method method) {
		return InvokerUtils.getRestPath(method);
	}

	@Override
	public String toString() {
		return "RemoteMethodConfig{" + //
				"method=" + method + //
				", version='" + version + '\'' + //
				", timeout=" + timeout + //
				", ignore=" + ignore + //
				", rest='" + rest + '\'' + //
				'}';
	}
}
