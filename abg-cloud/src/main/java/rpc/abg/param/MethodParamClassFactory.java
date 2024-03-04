package rpc.abg.param;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import rpc.abg.util.concurrent.ThreadLocalStringBuilder;

public final class MethodParamClassFactory {

	private static final String NOT_SUPPORT_PARAMETER_NAME_MSG = "must turn on \"Store information about method parameters (usable via reflection)\", see https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument";

	private static final ConcurrentMap<Method, Class<? extends MethodParam>> methodParamClassMap = new ConcurrentHashMap<>();

	/**
	 * 方法参数封装，用于序列化传输参数数据，其实现类会自动根据方法名称生成get/set方法，<br>
	 * 必须开启"Store information about method parameters (usable via reflection)"<br>
	 * 参考：https://www.concretepage.com/java/jdk-8/java-8-reflection-access-to-parameter-names-of-method-and-constructor-with-maven-gradle-and-eclipse-using-parameters-compiler-argument<br>
	 * <br>
	 * 方法public CompletableFuture&lt;User&gt; getUser(long id)
	 * 将会生成下面的MethodParamClass：
	 * 
	 * <pre>
	 * public class UserService_getUser_1_6f7c1a8cf867a945306fb82a78c0a191265b9786 implements MethodParam {
	 * 	private long id;
	 * 
	 * 	public long $param0() {
	 * 		return this.id;
	 * 	}
	 * 
	 * 	public long getId() {
	 * 		return this.id;
	 * 	}
	 * 
	 * 	public void setId(long id) {
	 * 		this.id = id;
	 * 	}
	 * 
	 * 	public UserService_getUser_1_6f7c1a8cf867a945306fb82a78c0a191265b9786() {
	 * 	}
	 * 
	 * 	public UserService_getUser_1_6f7c1a8cf867a945306fb82a78c0a191265b9786(long id) {
	 * 		this.id = id;
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @param method
	 *            方法
	 * @return MethodParamClass
	 * 
	 * @throws CannotCompileException
	 * @throws NotFoundException
	 * 
	 * @author Annabergite
	 */
	public static Class<? extends MethodParam> createClass(Method method)
			throws CannotCompileException, NotFoundException {
		Objects.requireNonNull(method, "method must not be null");

		if (method.getParameterCount() == 0) {
			return EmptyMethodParam.class;
		}

		Class<? extends MethodParam> methodParamClass = methodParamClassMap.get(method);
		if (methodParamClass != null) {
			return methodParamClass;
		}

		synchronized (MethodParamClassFactory.class) {
			methodParamClass = methodParamClassMap.get(method);
			if (methodParamClass != null) {
				return methodParamClass;
			}

			methodParamClass = doCreateClass(method);
			methodParamClassMap.put(method, methodParamClass);
		}

		return methodParamClass;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends MethodParam> doCreateClass(Method method)
			throws CannotCompileException, NotFoundException {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Parameter[] parameters = method.getParameters();

		if (!parameters[0].isNamePresent()) {
			throw new RuntimeException(NOT_SUPPORT_PARAMETER_NAME_MSG);
		}

		String paramTypes = Stream.of(parameterTypes)//
				.map(clazz -> clazz.getName())//
				.collect(Collectors.joining(",", "(", ")"));

		String hash = Hashing.murmur3_128().hashString(paramTypes, StandardCharsets.UTF_8).toString();

		final String methodParamClassName = method.getDeclaringClass().getName()//
				+ "$MethodParam"//
				+ "$" + method.getName()//
				+ "$" + parameterTypes.length//
				+ "$" + hash;// 防止同名方法冲突

		try {
			Class<?> clazz = MethodParamClassFactory.class.getClassLoader().loadClass(methodParamClassName);

			if (clazz != null) {
				return (Class<? extends MethodParam>) clazz;
			}
		} catch (ClassNotFoundException e) {
		}

		// 创建类
		ClassPool pool = ClassPool.getDefault();
		CtClass methodParamCtClass = pool.makeClass(methodParamClassName);

		CtClass[] interfaces = { pool.getCtClass(MethodParam.class.getName()) };
		methodParamCtClass.setInterfaces(interfaces);

		for (int i = 0; i < parameterTypes.length; i++) {
			Parameter parameter = parameters[i];

			String paramName = parameter.getName();
			Class<?> paramType = parameterTypes[i];

			String capitalize = Character.toUpperCase(paramName.charAt(0)) + paramName.substring(1);
			String getter = "get" + capitalize;
			String setter = "set" + capitalize;

			CtField ctField = new CtField(pool.get(paramType.getName()), paramName, methodParamCtClass);
			ctField.setModifiers(Modifier.PRIVATE);
			methodParamCtClass.addField(ctField);

			methodParamCtClass.addMethod(CtNewMethod.getter("$param" + i, ctField));
			methodParamCtClass.addMethod(CtNewMethod.getter(getter, ctField));
			methodParamCtClass.addMethod(CtNewMethod.setter(setter, ctField));
		}

		// 添加无参的构造函数
		CtConstructor constructor0 = new CtConstructor(null, methodParamCtClass);
		constructor0.setModifiers(Modifier.PUBLIC);
		constructor0.setBody("{}");
		methodParamCtClass.addConstructor(constructor0);

		// 添加有参的构造函数
		CtClass[] paramCtClassArray = new CtClass[method.getParameterCount()];
		for (int i = 0; i < method.getParameterCount(); i++) {
			Class<?> paramType = parameterTypes[i];
			CtClass paramCtClass = pool.get(paramType.getName());
			paramCtClassArray[i] = paramCtClass;
		}

		StringBuilder bodyBuilder = ThreadLocalStringBuilder.current();

		bodyBuilder.append("{\r\n");

		for (int i = 0; i < method.getParameterCount(); i++) {
			String paramName = parameters[i].getName();

			bodyBuilder.append("$0.");
			bodyBuilder.append(paramName);
			bodyBuilder.append(" = $");
			bodyBuilder.append(i + 1);
			bodyBuilder.append(";\r\n");
		}

		bodyBuilder.append("}");

		CtConstructor constructor1 = new CtConstructor(paramCtClassArray, methodParamCtClass);
		constructor1.setBody(bodyBuilder.toString());
		methodParamCtClass.addConstructor(constructor1);

		return (Class<? extends MethodParam>) methodParamCtClass.toClass();
	}

}
