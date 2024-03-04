package rpc.abg.util.concurrent;

import static rpc.abg.util.TableUtils.offset;
import static rpc.abg.util.TableUtils.tableSizeFor;
import static rpc.abg.util.UnsafeUtils.unsafe;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 高性能，仅适用于key值比较少的情况，不能超过512k个
 * 
 * @author Annabergite
 *
 */
public class ConcurrentIntToObjectArrayMap<T> {
	private static final Object NOT_FOUND = new Object();
	private static final int MAXIMUM_CAPACITY = 1024 * 512;

	private static final int ABASE;
	private static final int ASHIFT;

	private volatile Object[] array;

	public ConcurrentIntToObjectArrayMap() {
		this(16);
	}

	public ConcurrentIntToObjectArrayMap(int initialCapacity) {
		if (initialCapacity < 2) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}

		if (initialCapacity > MAXIMUM_CAPACITY) {
			throw new IndexOutOfBoundsException("Illegal initial capacity: " + initialCapacity);
		}

		ensureCapacity(initialCapacity);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public boolean contains(int key) {
		if (key < 0) {
			return false;
		}

		Object[] finalArray = array;
		if (key >= finalArray.length) {
			return false;
		}

		Object value = unsafe().getObjectVolatile(finalArray, offset(ABASE, ASHIFT, key));

		if (value == NOT_FOUND) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 
	 * @param key
	 * @return null为找不到
	 */
	@SuppressWarnings("unchecked")
	public T get(int key) {
		if (key < 0) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}

		Object[] finalArray = array;
		if (key >= finalArray.length) {
			return null;
		}

		Object value = unsafe().getObjectVolatile(finalArray, offset(ABASE, ASHIFT, key));

		if (value != NOT_FOUND) {
			return (T) value;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public T getOrUpdate(int key, Supplier<T> producer) {
		if (key < 0) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}

		Object value;
		final Object[] finalArray = array;

		if (key >= finalArray.length) {
			value = NOT_FOUND;
		} else {
			value = unsafe().getObjectVolatile(finalArray, offset(ABASE, ASHIFT, key));
		}

		if (value != NOT_FOUND) {
			return (T) value;
		}

		synchronized (this) {
			final Object[] theArray = array;
			if (key < theArray.length) {
				value = unsafe().getObjectVolatile(theArray, offset(ABASE, ASHIFT, key));
			}

			if (value != NOT_FOUND) {
				return (T) value;
			}

			value = producer.get();
			put(key, (T) value);

			return (T) value;
		}
	}

	/**
	 * 
	 * @param key
	 *            大于零，小于512k
	 * 
	 * @param value
	 */
	public void put(int key, T value) {
		if (key < 0) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}

		if (key >= MAXIMUM_CAPACITY) {
			throw new IndexOutOfBoundsException("Illegal key: " + key);
		}

		ensureCapacity(key + 1);
		final long offset = offset(ABASE, ASHIFT, key);

		for (;;) {// like cas
			final Object[] before = array;
			unsafe().putOrderedObject(before, offset, value);
			final Object[] after = array;

			if (before == after) {
				return;
			}
		}
	}

	/**
	 * 重置为未赋值
	 * 
	 * @param key
	 *            大于零，小于512k
	 */
	public boolean remove(final int key) {
		if (key < 0) {
			return false;
		}

		Object[] finalArray = array;
		if (key >= finalArray.length) {
			return false;
		}

		final long offset = offset(ABASE, ASHIFT, key);

		for (;;) {// like cas
			final Object[] before = array;
			unsafe().putOrderedObject(before, offset, NOT_FOUND);
			final Object[] after = array;

			if (before == after) {
				return true;
			}
		}
	}

	public void clear() {
		if (array == null) {
			return;
		}

		Object[] objs = new Object[16];
		Arrays.fill(objs, NOT_FOUND);

		array = objs;
	}

	private void ensureCapacity(int capacity) {
		Object[] theArray = array;
		if (theArray != null && theArray.length >= capacity) {
			return;
		}

		synchronized (this) {
			Object[] finalArray = array;
			if (finalArray != null && finalArray.length >= capacity) {
				return;
			}

			int newCapacity = tableSizeFor(capacity);

			if (newCapacity > MAXIMUM_CAPACITY) {
				throw new IndexOutOfBoundsException(newCapacity);
			}

			Object[] objs = new Object[newCapacity];
			Arrays.fill(objs, NOT_FOUND);

			if (finalArray != null) {
				System.arraycopy(finalArray, 0, objs, 0, finalArray.length);
			}

			array = objs;
		}
	}

	static {
		try {
			ABASE = unsafe().arrayBaseOffset(Object[].class);

			int scale = unsafe().arrayIndexScale(Object[].class);
			if ((scale & (scale - 1)) != 0) {
				throw new Error("array index scale not a power of two");
			}

			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	public static void main(String[] args) {
		ConcurrentIntToObjectArrayMap<Integer> map = new ConcurrentIntToObjectArrayMap<>();
		map.put(16, 16);

		for (int i = 0; i < 1024; i++) {
			map.put(i, i);
		}

		for (int i = 0; i < 1024; i++) {
			System.out.println(i + ":" + map.get(i));
		}
	}

}
