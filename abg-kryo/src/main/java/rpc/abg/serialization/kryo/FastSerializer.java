package rpc.abg.serialization.kryo;

import static rpc.abg.util.ReflectUtils.getGetMethod;
import static rpc.abg.util.ReflectUtils.getSetMethod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.UnsafeUtil;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import rpc.abg.util.SingleClassLoader;
import rpc.abg.util.tuple.Tuple;
import rpc.abg.util.tuple.Tuple2;

/**
 * 基于 Javassist 实现的 Serializer
 * 
 * @author Annabergite
 *
 * @param <T>
 */
public class FastSerializer<T> extends Serializer<T> {
	private static final String CHILD_REGISTRATION_PREFIX = "childRegistration$";

	private final Kryo kryo;
	private final Class<T> type;
	private final Method[] methods;
	private final Field[] fields;
	private final Serializer<T> realSerializer;

	// 生成 realSerializer 的过程中临时使用
	private Map<Class<?>, Tuple2<Registration, Integer>> childRegistrationMap;

	public FastSerializer(Kryo kryo, Class<T> type) {
		this.kryo = kryo;
		this.type = type;
		this.methods = type.getMethods();
		this.fields = getAllFields(type);

		try {
			this.realSerializer = generateRealSerializer();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(Kryo kryo, Output output, T object) {
		realSerializer.write(kryo, output, object);
	}

	@Override
	public T read(Kryo kryo, Input input, Class<T> type) {
		return realSerializer.read(kryo, input, type);
	}

	private Field[] getAllFields(Class<T> type) {
		List<Field> allFields = new ArrayList<>();

		Class<?> nextClass = type;
		while (nextClass != Object.class) {
			Field[] declaredFields = nextClass.getDeclaredFields();

			if (declaredFields != null) {
				for (Field f : declaredFields) {

					int modifiers = f.getModifiers();
					if (Modifier.isStatic(modifiers)) {
						continue;
					}

					if (Modifier.isFinal(modifiers)) {
						continue;
					}

					if (Modifier.isTransient(modifiers)) {
						continue;
					}

					if (!Modifier.isPublic(modifiers)) {
						// 如果不是public的，就必须有 get、set 方法

						if (getGetMethod(methods, f) == null) {
							continue;
						}

						if (getSetMethod(methods, f) == null) {
							continue;
						}
					}

					allFields.add(f);
				}
			}

			nextClass = nextClass.getSuperclass();
		}

		return UnsafeUtil.sortFieldsByOffset(allFields);
	}

	@SuppressWarnings("unchecked")
	private Serializer<T> generateRealSerializer() throws Exception {
		Objects.nonNull(type);

		final String serializerClassName = "rpc.abg.serialization.kryo.FastSerializer_"//
				+ UUID.randomUUID().toString().replace("-", "");

		// 创建类
		ClassPool pool = ClassPool.getDefault();
		CtClass serializerCtClass = pool.makeClass(serializerClassName);
		serializerCtClass.setSuperclass(pool.getCtClass(Serializer.class.getName()));

		// 添加无参的构造函数
		CtConstructor constructor0 = new CtConstructor(null, serializerCtClass);
		constructor0.setModifiers(Modifier.PUBLIC);
		constructor0.setBody("{}");
		serializerCtClass.addConstructor(constructor0);

		String writeMethodCode = generateWriteMethod();
		String readMethodCode = generateReadMethod();

		// 添加字段childRegistration$0
		if (childRegistrationMap != null && childRegistrationMap.size() > 0) {
			for (Tuple2<Registration, Integer> kv : childRegistrationMap.values()) {
				String fieldName = CHILD_REGISTRATION_PREFIX + kv._2;

				CtField ctField = new CtField(pool.get(Registration.class.getName()), fieldName, serializerCtClass);
				ctField.setModifiers(Modifier.PRIVATE);
				serializerCtClass.addField(ctField);
			}
		}

		// 添加write方法
		CtMethod writeMethod = CtNewMethod.make(writeMethodCode, serializerCtClass);
		serializerCtClass.addMethod(writeMethod);

		// 添加read方法
		CtMethod readMethod = CtNewMethod.make(readMethodCode, serializerCtClass);
		serializerCtClass.addMethod(readMethod);

		byte[] bytes = serializerCtClass.toBytecode();
		Class<?> serializerClass = SingleClassLoader.loadClass(kryo.getClassLoader(), bytes);

		// 通过反射创建有参的实例
		Serializer<T> serializer = (Serializer<T>) serializerClass.getConstructor().newInstance();

		// 通过反射给字段赋值
		if (childRegistrationMap != null && childRegistrationMap.size() > 0) {
			for (Tuple2<Registration, Integer> kv : childRegistrationMap.values()) {
				String fieldName = CHILD_REGISTRATION_PREFIX + kv._2;

				Field field = serializerClass.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(serializer, kv._1);
			}

			childRegistrationMap = null;
		}

		return serializer;
	}

	private Tuple2<Registration, Integer> getRegistration(Class<?> type) {
		if (childRegistrationMap == null) {
			childRegistrationMap = new HashMap<>();
		}

		Tuple2<Registration, Integer> childRegistration = childRegistrationMap.get(type);
		if (childRegistration != null) {
			return childRegistration;
		}

		Registration registration = kryo.getRegistration(type);
		childRegistration = Tuple.tuple(registration, childRegistrationMap.size());

		childRegistrationMap.put(type, childRegistration);

		return childRegistration;
	}

	// 写入output开始

	private String generateWriteMethod() {
		StringBuilder builder = new StringBuilder();

		builder.append(
				"public void write(com.esotericsoftware.kryo.Kryo kryo, com.esotericsoftware.kryo.io.Output output, java.lang.Object object) {\r\n");

		builder.append(type.getName());
		builder.append(" target = (");
		builder.append(type.getName());
		builder.append(")object;\r\n\r\n");

		for (Field field : fields) {
			appendWriteField(builder, field);
			builder.append("\r\n");
		}

		builder.append("\r\n}");

		return builder.toString();
	}

	private void appendWriteField(StringBuilder builder, Field field) {
		Class<?> type = field.getType();

		if (type == int.class) {
			// output.writeInt(target.fieldName, false)
			builder.append("output.writeInt(");
			appendGetFieldValue(builder, field);
			builder.append(", false);");
			return;
		}

		if (type == String.class) {
			// output.writeString(target.fieldName)
			builder.append("output.writeString(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == long.class) {
			// output.writeLong(target.fieldName, false)
			builder.append("output.writeLong(");
			appendGetFieldValue(builder, field);
			builder.append(", false);");
			return;
		}

		if (type == boolean.class) {
			// output.writeBoolean(target.fieldName)
			builder.append("output.writeBoolean(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == double.class) {
			// output.writeDouble(target.fieldName)
			builder.append("output.writeDouble(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == float.class) {
			// output.writeFloat(target.fieldName)
			builder.append("output.writeFloat(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == short.class) {
			// output.writeShort(target.fieldName)
			builder.append("output.writeShort(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == byte.class) {
			// output.writeByte(target.fieldName)
			builder.append("output.writeByte(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		if (type == char.class) {
			// output.getChar(target.fieldName)
			builder.append("output.writeChar(");
			appendGetFieldValue(builder, field);
			builder.append(");");
			return;
		}

		// 其他复杂类型
		boolean isFinal = Modifier.isFinal(type.getModifiers());
		Tuple2<Registration, Integer> registration = getRegistration(type);
		int id = registration._2;

		if (isFinal) {// 不需要写入className
			// kryo.writeObjectOrNull(output, value, serializer);
			builder.append("kryo.writeObjectOrNull(output, ");
			appendGetFieldValue(builder, field);
			builder.append(", ");
			builder.append(CHILD_REGISTRATION_PREFIX);
			builder.append(id);
			builder.append(".getSerializer());");
		} else {
			// FastSerializer.fastWrite(kryo, output, defaultRegistration, value);
			builder.append(FastSerializer.class.getName());
			builder.append(".fastWrite(kryo, output, ");
			builder.append(CHILD_REGISTRATION_PREFIX);
			builder.append(id);
			builder.append(", ");
			appendGetFieldValue(builder, field);
			builder.append(");");
		}
	}

	/**
	 * 快速写入
	 * 
	 * @param kryo
	 * @param output
	 * @param defaultRegistration
	 * @param value
	 */
	public static void fastWrite(Kryo kryo, Output output, Registration defaultRegistration, Object value) {
		if (value == null) {
			kryo.writeClass(output, null);
			return;
		}

		Class<?> type = value.getClass();

		if (defaultRegistration.getType().equals(type)) {
			if (defaultRegistration.getId() == FastClassResolver.NAME) {
				((FastClassResolver) kryo.getClassResolver()).writeName(output, type, defaultRegistration);
			} else {
				output.writeVarInt(defaultRegistration.getId() + 2, true);
			}

			kryo.writeObject(output, value, defaultRegistration.getSerializer());
		} else {
			Registration registration = kryo.writeClass(output, value.getClass());
			Serializer<?> serializer = registration.getSerializer();
			kryo.writeObject(output, value, serializer);
		}
	}

	// 写入output结束

	// 读取input开始

	private void appendGetFieldValue(StringBuilder builder, Field field) {
		String name = field.getName();
		boolean isPublic = Modifier.isPublic(field.getModifiers());

		if (isPublic) {// 直接字段访问
			builder.append("target.");
			builder.append(name);
		} else {// 通过get方法访问
			Method getMethod = getGetMethod(methods, field);

			builder.append("target.");
			builder.append(getMethod.getName());
			builder.append("()");
		}
	}

	private String generateReadMethod() {
		StringBuilder builder = new StringBuilder();

		builder.append(
				"public java.lang.Object read(com.esotericsoftware.kryo.Kryo kryo, com.esotericsoftware.kryo.io.Input input, java.lang.Class type) {\r\n");

		builder.append(type.getName());
		builder.append(" target = new ");
		builder.append(type.getName());
		builder.append("();\r\n\r\n");

		for (Field field : fields) {
			appendSetFieldValue(builder, field);
			builder.append("\r\n");
		}

		builder.append("\r\nreturn target;\r\n}");

		return builder.toString();
	}

	private void appendSetFieldValue(StringBuilder builder, Field field) {
		String name = field.getName();
		boolean isPublic = Modifier.isPublic(field.getModifiers());

		if (isPublic) {// 直接字段访问
			builder.append("target.");
			builder.append(name);
			builder.append(" = ");
			appendReadFieldCode(builder, field);
			builder.append(";");
		} else {// 通过set方法访问
			Method setMethod = getSetMethod(methods, field);

			builder.append("target.");
			builder.append(setMethod.getName());
			builder.append("(");
			appendReadFieldCode(builder, field);
			builder.append(");");
		}
	}

	private void appendReadFieldCode(StringBuilder builder, Field field) {
		Class<?> type = field.getType();

		if (type == int.class) {
			builder.append("input.readInt(false)");
			return;
		}

		if (type == String.class) {
			builder.append("input.readString()");
			return;
		}

		if (type == long.class) {
			builder.append("input.readLong(false)");
			return;
		}

		if (type == boolean.class) {
			builder.append("input.readBoolean()");
			return;
		}

		if (type == double.class) {
			builder.append("input.readDouble()");
			return;
		}

		if (type == float.class) {
			builder.append("input.readFloat()");
			return;
		}

		if (type == short.class) {
			builder.append("input.readShort()");
			return;
		}

		if (type == byte.class) {
			builder.append("input.readByte()");
			return;
		}

		if (type == char.class) {
			builder.append("input.getChar()");
			return;
		}

		// 其他复杂类型
		boolean isFinal = Modifier.isFinal(type.getModifiers());
		Tuple2<Registration, Integer> registration = getRegistration(type);
		int id = registration._2;

		// 强制类型转换
		builder.append("(");
		builder.append(type.getName());
		builder.append(")");

		if (isFinal) {// 不需要写入className
			// kryo.readObjectOrNull(input, type, serializer);
			builder.append("kryo.readObjectOrNull(input, ");
			builder.append(type.getName());
			builder.append(".class, ");
			builder.append(CHILD_REGISTRATION_PREFIX);
			builder.append(id);
			builder.append(".getSerializer())");
		} else {
			// FastSerializer.slowRead(kryo, input);
			builder.append(FastSerializer.class.getName());
			builder.append(".slowRead(kryo, input)");
		}
	}

	@SuppressWarnings("unchecked")
	public static Object slowRead(Kryo kryo, Input input) {
		Registration registration = kryo.readClass(input);
		if (registration == null) {
			return null;
		} else {
			Serializer<?> serializer = registration.getSerializer();
			return kryo.readObject(input, registration.getType(), serializer);
		}
	}

	// 读取input结束

}
