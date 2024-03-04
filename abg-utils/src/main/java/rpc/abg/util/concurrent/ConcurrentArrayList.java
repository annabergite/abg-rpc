package rpc.abg.util.concurrent;

import static rpc.abg.util.TableUtils.offset;
import static rpc.abg.util.TableUtils.tableSizeFor;
import static rpc.abg.util.UnsafeUtils.unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * only support add get set, not support remove
 * 
 * @author Annabergite
 *
 * @param <T>
 */
public class ConcurrentArrayList<T> implements RandomAccess {
	public static final int MAXIMUM_CAPACITY = 1 << 30;

	private static final long SIZE_OFFSET;
	private static final int ABASE;
	private static final int ASHIFT;

	private volatile Object[] values;
	private volatile int size;// unsafe atomic operate

	public ConcurrentArrayList() {
		this(16);
	}

	public ConcurrentArrayList(int initialCapacity) {
		if (initialCapacity < 2) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}

		if (initialCapacity > MAXIMUM_CAPACITY) {
			throw new IndexOutOfBoundsException("Illegal initial capacity: " + initialCapacity);
		}

		ensureCapacity(initialCapacity);
	}

	public void add(T value) {
		final int index = insertIndex();
		final long offset = offset(ABASE, ASHIFT, index);

		for (;;) {// like cas
			final Object[] before = values;
			unsafe().putOrderedObject(before, offset, value);
			final Object[] after = values;

			if (before == after) {
				return;
			}
		}
	}

	public void addAll(Collection<T> collection) {
		if (collection == null) {
			return;
		}

		ensureCapacity(size + collection.size());

		for (T t : collection) {
			add(t);
		}
	}

	@SuppressWarnings("unchecked")
	public T get(int index) {
		Objects.checkIndex(index, size);
		return (T) unsafe().getObjectVolatile(values, offset(ABASE, ASHIFT, index));
	}

	public void set(int index, T value) {
		Objects.checkIndex(index, size);
		final long offset = offset(ABASE, ASHIFT, index);

		for (;;) {// like cas
			final Object[] before = values;
			unsafe().putOrderedObject(before, offset, value);
			final Object[] after = values;

			if (before == after) {
				return;
			}
		}
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	public ArrayList<T> toArrayList() {
		int finalSize = size;
		ArrayList<T> list = new ArrayList<>(finalSize);

		for (int i = 0; i < finalSize; i++) {
			list.add(get(i));
		}

		return list;
	}

	private int insertIndex() {
		int index = unsafe().getAndAddInt(this, SIZE_OFFSET, 1);
		ensureCapacity(index + 1);

		return index;
	}

	private void ensureCapacity(int capacity) {
		Object[] theArray = values;
		if (theArray != null && theArray.length >= capacity) {
			return;
		}

		synchronized (this) {
			Object[] finalArray = values;
			if (finalArray != null && finalArray.length >= capacity) {
				return;
			}

			int newCapacity = tableSizeFor(capacity);

			if (newCapacity > MAXIMUM_CAPACITY) {
				throw new IndexOutOfBoundsException(newCapacity);
			}

			Object[] objs = new Object[newCapacity];

			if (finalArray != null) {
				System.arraycopy(finalArray, 0, objs, 0, finalArray.length);
			}

			values = objs;
		}
	}

	static {
		try {
			Field field = ConcurrentArrayList.class.getDeclaredField("size");
			SIZE_OFFSET = unsafe().objectFieldOffset(field);

			ABASE = unsafe().arrayBaseOffset(Object[].class);

			int scale = unsafe().arrayIndexScale(Object[].class);
			if ((scale & (scale - 1)) != 0) {
				throw new Error("array index scale not a power of two");
			}

			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	public static void main(String[] args) {
		ConcurrentArrayList<Integer> list = new ConcurrentArrayList<>();

		for (int i = 0; i < 1024; i++) {
			list.add(i);
		}

		for (int i = 0; i < 1024; i++) {
			System.out.println(i + ":" + list.get(i));
		}

		for (int i = 0; i < 1024; i++) {
			list.set(i, i + 10000);
		}

		for (int i = 0; i < 1024; i++) {
			System.out.println(i + ":" + list.get(i));
		}

	}
}
