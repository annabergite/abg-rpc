package rpc.abg.util;

import static rpc.abg.util.TableUtils.tableSizeFor;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 高性能，仅适用于key值比较少的情况，不能超过512k个
 * 
 * @author Annabergite
 *
 */
public class IntToObjectArrayMap<T> {
	public static final Object NOT_FOUND = new Object();
	public static final int MAXIMUM_CAPACITY = 1024 * 512;

	private Object[] array;

	public IntToObjectArrayMap() {
		this(16);
	}

	public IntToObjectArrayMap(int initialCapacity) {
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

		Object value = finalArray[key];

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

		Object value = finalArray[key];

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

		final Object[] finalArray = array;

		Object value;
		if (key >= finalArray.length) {
			value = NOT_FOUND;
		} else {
			value = finalArray[key];
		}

		if (value != NOT_FOUND) {
			return (T) value;
		}

		value = producer.get();
		put(key, (T) value);

		return (T) value;
	}

	/**
	 * 
	 * @param key
	 *            大于零，小于256k
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

		array[key] = value;
	}

	public void clear() {
		if (array == null) {
			return;
		}

		Arrays.fill(array, NOT_FOUND);
	}

	private void ensureCapacity(int capacity) {
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

	public static void main(String[] args) {
		IntToObjectArrayMap<Integer> map = new IntToObjectArrayMap<>();
		map.put(16, 16);

		for (int i = 0; i < 1024; i++) {
			map.put(i, i);
		}

		for (int i = 0; i < 1024; i++) {
			System.out.println(i + ":" + map.get(i));
		}
	}

}
