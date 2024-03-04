package rpc.abg.serialization;

/**
 * 序列化器工厂方法
 * 
 * @author Annabergite
 *
 */
public final class SerializerFactory {

	/**
	 * 创建一个 Serializer
	 * 
	 * @param className
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Serializer createSerializer(String className) {
		try {
			Class<?> clazz = Class.forName(className);

			if (!Serializer.class.isAssignableFrom(clazz)) {
				throw new RuntimeException(className + " is not instance of Serializer");
			}

			return createSerializer((Class<? extends Serializer>) clazz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 创建一个 Serializer
	 * 
	 * @param clazz
	 * @return
	 */
	public static Serializer createSerializer(Class<? extends Serializer> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
