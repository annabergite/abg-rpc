package rpc.abg.invoke;

import static rpc.abg.util.SourceCodeUtils.box;
import static rpc.abg.util.SourceCodeUtils.forceCast;
import static rpc.abg.util.SourceCodeUtils.unbox;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import rpc.abg.param.EmptyMethodParam;
import rpc.abg.param.MethodParam;
import rpc.abg.param.MethodParamClassFactory;
import rpc.abg.util.SingleClassLoader;
import rpc.abg.util.TypeUtils;

/**
 * high performance invoker
 * 
 * @author Annabergite.whu@gmail.com
 *
 * @param <T>
 *            the method return type
 */
public class JavassistInvoker<T> implements Invoker<T> {
	private static final String NOT_SUPPORT_PARAMETER_NAME_MSG = "must turn on \"Store information about method parameters (usable via reflection)\", see https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument";

	final int serviceId;
	private final Object service;
	final Class<?> clazz;
	final Method method;
	private final Class<?>[] parameterTypes;
	private final String[] parameterNames;
	private final int parameterCount;

	final Class<? extends MethodParam> methodParamClass;
	private final Invoker<T> realInvoker;

	private final boolean supportHttpForm;

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
	 * @throws InvokeException
	 */
	public JavassistInvoker(int serviceId, Object service, Class<?> clazz, Method method) {
		this.serviceId = serviceId;
		this.service = service;
		this.clazz = clazz;
		this.method = method;
		this.parameterTypes = method.getParameterTypes();
		this.parameterCount = parameterTypes.length;

		if (service == null) {
			throw new InvokeException("service cannot be null");
		}

		if (!clazz.equals(method.getDeclaringClass())) {
			throw new InvokeException(clazz + " have no method: " + method);
		}

		if (!Modifier.isPublic(clazz.getModifiers())) {
			throw new InvokeException("the method must be public");
		}

		try {
			methodParamClass = MethodParamClassFactory.createClass(method);
		} catch (Exception e) {
			throw new InvokeException(e);
		}

		try {
			this.realInvoker = generateRealInvoker();
		} catch (Exception e) {
			throw new InvokeException(e);
		}

		boolean _supportHttpForm = true;
		for (int i = 0; i < parameterCount; i++) {
			Class<?> paramType = parameterTypes[i];

			if (!TypeUtils.supportCast(paramType)) {
				_supportHttpForm = false;
				break;
			}
		}

		supportHttpForm = _supportHttpForm;

		parameterNames = new String[parameterCount];
		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameterCount; i++) {
			Parameter parameter = parameters[i];

			if (!parameter.isNamePresent()) {
				throw new RuntimeException(NOT_SUPPORT_PARAMETER_NAME_MSG);
			}

			parameterNames[i] = parameter.getName();
		}
	}

	/**
	 * 
	 * @throws InvokeException
	 */
	@Override
	public T invoke(Object... params) {
		if (params == null) {
			if (parameterCount != 0) {
				throw new InvokeException(method.getName() + " params count error, params is null");
			}
		} else if (parameterCount != params.length) {
			throw new InvokeException(method.getName() + " params count error, " + Arrays.toString(params));
		}

		return realInvoker.invoke(params);
	}

	@Override
	public T invoke() {
		return realInvoker.invoke();
	}

	public T invoke(MethodParam methodParam) {
		if (methodParam == null || methodParam instanceof EmptyMethodParam) {
			return realInvoker.invoke();
		}

		if (!methodParamClass.isInstance(methodParam)) {
			throw new IllegalArgumentException("methodParam not instanceof " + methodParamClass.getName());
		}

		return realInvoker.invoke(methodParam);
	}

	@Override
	public T invoke(Object params0) {
		return realInvoker.invoke(params0);
	}

	@Override
	public T invoke(Object param0, Object param1) {
		return realInvoker.invoke(param0, param1);
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2) {
		return realInvoker.invoke(param0, param1, param2);
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3) {
		return realInvoker.invoke(param0, param1, param2, param3);
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3, Object param4) {
		return realInvoker.invoke(param0, param1, param2, param3, param4);
	}

	@Override
	public T invoke(Object param0, Object param1, Object param2, Object param3, Object param4, Object param5) {
		return realInvoker.invoke(param0, param1, param2, param3, param4, param5);
	}

	@Override
	public int getServiceId() {
		return serviceId;
	}

	@Override
	public Method getMethod() {
		return method;
	}

	@Override
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Class<? extends MethodParam> getMethodParamClass() {
		return methodParamClass;
	}

	@Override
	public boolean supportHttpForm() {
		return supportHttpForm;
	}

	private Invoker<T> generateRealInvoker() throws Exception {
		final String invokerClassName = "rpc.abg.invoke.generate.Invoker_"//
				+ serviceId + "_" //
				+ UUID.randomUUID().toString().replace("-", "");

		// 创建类
		ClassPool pool = ClassPool.getDefault();
		CtClass invokerCtClass = pool.makeClass(invokerClassName);
		invokerCtClass.setInterfaces(new CtClass[] { pool.getCtClass(Invoker.class.getName()) });

		// 添加私有成员service
		CtField serviceField = new CtField(pool.get(service.getClass().getName()), "service", invokerCtClass);
		serviceField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
		invokerCtClass.addField(serviceField);

		// 添加有参的构造函数
		CtConstructor constructor = new CtConstructor(new CtClass[] { pool.get(service.getClass().getName()) },
				invokerCtClass);
		constructor.setBody("{$0.service = $1;}");
		invokerCtClass.addConstructor(constructor);

		{// 添加MethodParam方法
			StringBuilder methodBuilder = new StringBuilder();
			StringBuilder resultBuilder = new StringBuilder();

			methodBuilder.append("public Object invoke(rpc.abg.param.MethodParam methodParam) {\r\n");

			if (parameterTypes.length > 0) {
				// 强制类型转换
				methodBuilder.append(methodParamClass.getName());
				methodBuilder.append("  params = (");
				methodBuilder.append(methodParamClass.getName());
				methodBuilder.append(")methodParam;");
			}

			methodBuilder.append("  return ");

			resultBuilder.append("service.");
			resultBuilder.append(method.getName());
			resultBuilder.append("(");

			for (int i = 0; i < parameterTypes.length; i++) {
				resultBuilder.append("params.$param");
				resultBuilder.append(i);
				resultBuilder.append("()");

				if (i != parameterTypes.length - 1) {
					methodBuilder.append(", ");
				}
			}

			resultBuilder.append(")");

			String resultStr = box(method.getReturnType(), resultBuilder.toString());

			methodBuilder.append(resultStr);
			methodBuilder.append(";\r\n}");

			CtMethod m = CtNewMethod.make(methodBuilder.toString(), invokerCtClass);
			invokerCtClass.addMethod(m);
		}

		{// 添加通用方法
			StringBuilder methodBuilder = new StringBuilder();
			StringBuilder resultBuilder = new StringBuilder();

			methodBuilder.append("public Object invoke(Object[] params) {\r\n");

			methodBuilder.append("  return ");

			resultBuilder.append("service.");
			resultBuilder.append(method.getName());
			resultBuilder.append("(");

			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> paramType = parameterTypes[i];

				resultBuilder.append("((");
				resultBuilder.append(forceCast(paramType));

				resultBuilder.append(")params[");
				resultBuilder.append(i);
				resultBuilder.append("])");

				resultBuilder.append(unbox(paramType));
			}

			resultBuilder.append(")");

			String resultStr = box(method.getReturnType(), resultBuilder.toString());

			methodBuilder.append(resultStr);
			methodBuilder.append(";\r\n}");

			CtMethod m = CtNewMethod.make(methodBuilder.toString(), invokerCtClass);
			invokerCtClass.addMethod(m);
		}

		if (parameterCount <= 6) {// just for benchmark
			StringBuilder methodBuilder = new StringBuilder();
			StringBuilder resultBuilder = new StringBuilder();

			methodBuilder.append("public Object invoke(");

			String params = IntStream//
					.range(0, parameterCount)//
					.mapToObj(i -> "Object param" + i)//
					.collect(Collectors.joining(","));

			methodBuilder.append(params);

			methodBuilder.append(") {\r\n");

			methodBuilder.append("  return ");

			resultBuilder.append("service.");
			resultBuilder.append(method.getName());
			resultBuilder.append("(");

			for (int i = 0; i < parameterCount; i++) {
				Class<?> paramType = parameterTypes[i];

				resultBuilder.append("((");
				resultBuilder.append(forceCast(paramType));

				resultBuilder.append(")param");
				resultBuilder.append(i);
				resultBuilder.append(")");

				resultBuilder.append(unbox(paramType));

				if (i != parameterCount - 1) {
					resultBuilder.append(",");
				}
			}

			resultBuilder.append(")");

			String resultStr = box(method.getReturnType(), resultBuilder.toString());

			methodBuilder.append(resultStr);
			methodBuilder.append(";\r\n}");

			CtMethod m = CtNewMethod.make(methodBuilder.toString(), invokerCtClass);
			invokerCtClass.addMethod(m);
		}

		byte[] bytes = invokerCtClass.toBytecode();
		Class<?> invokerClass = SingleClassLoader.loadClass(service.getClass().getClassLoader(), bytes);

		// 通过反射创建有参的实例
		@SuppressWarnings("unchecked")
		Invoker<T> invoker = (Invoker<T>) invokerClass.getConstructor(service.getClass()).newInstance(service);

		return invoker;
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

		JavassistInvoker<?> other = (JavassistInvoker<?>) obj;
		if (serviceId != other.serviceId)
			return false;

		return true;
	}

}
