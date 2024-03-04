package rpc.abg.util;

/**
 * 单个类的类加载器
 * 
 * @author Annabergite
 *
 */
public class SingleClassLoader extends ClassLoader {

	private final Class<?> clazz;

	public SingleClassLoader(ClassLoader parent, byte[] bytes) {
		super(parent);
		this.clazz = defineClass(null, bytes, 0, bytes.length, null);
	}

	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (clazz != null && clazz.getName().equals(name)) {
			return clazz;
		}

		return getParent().loadClass(name);
	}

	public static <T> Class<T> loadClass(byte[] bytes) {
		ClassLoader parent = SingleClassLoader.class.getClassLoader();
		return loadClass(parent, bytes);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> loadClass(ClassLoader parent, byte[] bytes) {
		return (Class<T>) new SingleClassLoader(parent, bytes).getClazz();
	}
}
