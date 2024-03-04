package rpc.abg.invoke;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 50% performance of JavassistInvoker
 * 
 * @author Annabergite
 *
 * @param <T>
 *            the method return type
 */
public class ReflectInvoker<T> implements Invoker<T> {
	public final int serviceId;
	private final Object service;
	public final Class<?> clazz;
	public final Method method;
	private final Class<?>[] parameterTypes;
	private final int parameterCount;

	/**
	 * 
	 * @param serviceId
	 * 
	 * @param service
	 *            the service implemention
	 * 
	 * @param clazz
	 *            the service interface
	 * 
	 * @param method
	 */
	public ReflectInvoker(int serviceId, Object service, Class<?> clazz, Method method) {
		this.serviceId = serviceId;
		this.service = service;
		this.clazz = clazz;
		this.method = method;
		this.parameterTypes = method.getParameterTypes();
		this.parameterCount = parameterTypes.length;

		if (service == null) {
			throw new InvokeException("service cannot be null");
		}

		if (!clazz.isInstance(service)) {
			throw new InvokeException("clazz is the interface, service is the implemention");
		}

		if (!clazz.equals(method.getDeclaringClass())) {
			throw new InvokeException(clazz + " have no method: " + method);
		}

		if (!Modifier.isPublic(clazz.getModifiers())) {
			throw new InvokeException("the method must be public");
		}
	}

	/**
	 * 
	 * @throws InvokeException
	 */
	@SuppressWarnings("unchecked")
	public T invoke(Object... params) {
		if (params == null) {
			if (parameterCount != 0) {
				throw new InvokeException("params count error");
			} else {
				try {
					return (T) method.invoke(service);
				} catch (Exception e) {
					throw new InvokeException(e);
				}
			}
		}

		if (parameterCount != params.length) {
			throw new InvokeException("params count error");
		}

		try {
			return (T) method.invoke(service, params);
		} catch (Exception e) {
			throw new InvokeException(e);
		}
	}

	@Override
	public int hashCode() {
		return serviceId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		ReflectInvoker<?> other = (ReflectInvoker<?>) obj;
		if (serviceId != other.serviceId)
			return false;

		return true;
	}

}
