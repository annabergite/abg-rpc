package rpc.abg.invoke;

import java.lang.reflect.Method;

import rpc.abg.param.MethodParam;

public interface Invoker<T> {

	public T invoke(Object... params);

	default T invoke() {
		return invoke(new Object[] {});
	}

	default T invoke(MethodParam methodParam) {
		throw new UnsupportedOperationException();
	}

	default T invoke(Object param0) {
		return invoke(new Object[] { param0 });
	}

	default T invoke(Object param0, Object param1) {
		return invoke(new Object[] { param0, param1 });
	}

	default T invoke(Object param0, Object param1, Object param2) {
		return invoke(new Object[] { param0, param1, param2 });
	}

	default T invoke(Object param0, Object param1, Object param2, Object param3) {
		return invoke(new Object[] { param0, param1, param2, param3 });
	}

	default T invoke(Object param0, Object param1, Object param2, Object param3, Object param4) {
		return invoke(new Object[] { param0, param1, param2, param3, param4 });
	}

	default T invoke(Object param0, Object param1, Object param2, Object param3, Object param4, Object param5) {
		return invoke(new Object[] { param0, param1, param2, param3, param4, param5 });
	}

	default public int getServiceId() {
		throw new UnsupportedOperationException();
	}

	default public Method getMethod() {
		throw new UnsupportedOperationException();
	}

	default public Class<?>[] getParameterTypes() {
		throw new UnsupportedOperationException();
	}

	default public String[] getParameterNames() {
		throw new UnsupportedOperationException();
	}

	default public Class<? extends MethodParam> getMethodParamClass() {
		throw new UnsupportedOperationException();
	}

	default public boolean supportHttpForm() {
		return false;
	}

}
