package rpc.abg.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * DO NOT USE THIS, BROKEN, NOT WORK
 * 
 * @author Annabergite
 *
 * @param <T>
 *            the method return type
 */
public class MethodHandleInvoker<T> implements Invoker<T> {
	public final int serviceId;
	private final Object service;
	public final Class<?> clazz;
	public final Method method;
	private final MethodHandle methodHandle;
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
	public MethodHandleInvoker(int serviceId, Object service, Class<?> clazz, Method method) {
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

		MethodType methodType = MethodType.methodType(method.getReturnType(), parameterTypes);
		try {
			methodHandle = MethodHandles.lookup().findVirtual(clazz, method.getName(), methodType);
		} catch (Exception e) {
			throw new InvokeException(e);
		}
	}

	/**
	 * DO NOT USE THIS, BROKEN, NOT WORK
	 * 
	 * @throws InvokeException
	 */
	public T invoke(Object... params) {
		if (params == null) {
			if (parameterCount != 0) {
				throw new InvokeException("params count error");
			} else {
				return invoke();
			}
		}

		if (parameterCount != params.length) {
			throw new InvokeException("params count error");
		}

		if (parameterCount > 6) {
			throw new InvokeException("params count error, only support 0~6 params");
		}

		try {
			Object[] args = new Object[parameterCount + 1];
			args[0] = service;
			System.arraycopy(params, 0, args, 1, params.length);
			return (T) methodHandle.invoke(args);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke() {
		try {
			return (T) methodHandle.invoke(service);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0) {
		try {
			return (T) methodHandle.invoke(service, param0);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0, Object param1) {
		try {
			return (T) methodHandle.invoke(service, param0, param1);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2) {
		try {
			return (T) methodHandle.invoke(service, param0, param1, param2);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3) {
		try {
			return (T) methodHandle.invoke(service, param0, param1, param2, param3);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3, Object param4) {
		try {
			return (T) methodHandle.invoke(service, param0, param1, param2, param3, param4);
		} catch (Throwable t) {
			throw new InvokeException(t);
		}
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3, Object param4, Object param5) {
		try {
			return (T) methodHandle.invoke(service, param0, param1, param2, param3, param4, param5);
		} catch (Throwable t) {
			throw new InvokeException(t);
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

		MethodHandleInvoker<?> other = (MethodHandleInvoker<?>) obj;
		if (serviceId != other.serviceId)
			return false;

		return true;
	}

}
