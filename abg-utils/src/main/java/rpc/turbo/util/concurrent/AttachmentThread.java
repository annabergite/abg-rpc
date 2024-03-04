package rpc.abg.util.concurrent;

import java.util.Arrays;
import java.util.function.Supplier;

import io.netty.util.concurrent.FastThreadLocalThread;

/**
 * just for long time running thread
 * 
 * @author Annabergite
 *
 */
public class AttachmentThread extends FastThreadLocalThread {
	public static final Object NOT_FOUND = new Object();
	public static final int MAXIMUM_CAPACITY = 1 << 30;

	// 线程本地变量
	private Object[] objs;

	public AttachmentThread() {
		super();
	}

	public AttachmentThread(Runnable runnable, String name) {
		super(runnable, name);
	}

	public AttachmentThread(Runnable runnable) {
		super(runnable);
	}

	public AttachmentThread(String name) {
		super(name);
	}

	public AttachmentThread(ThreadGroup threadGroup, Runnable runnable, String name) {
		super(threadGroup, runnable, name);
	}

	/**
	 * 存储线程变量
	 * 
	 * @param index
	 *            需要从AttachmentThreadUtils.nextVarIndex()获取到
	 * @param value
	 */
	public void put(int index, Object value) {
		if (index < 0) {
			throw new IllegalArgumentException("Illegal index: " + index);
		}

		if (index >= MAXIMUM_CAPACITY) {
			throw new IndexOutOfBoundsException("Illegal index: " + index);
		}

		ensureCapacity(index + 1);

		objs[index] = value;
	}

	/**
	 * 获取线程变量
	 * 
	 * @param index
	 *            需要从 AttachmentThreadUtils.nextVarIndex()获取到
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("Illegal index: " + index);
		}

		if (objs == null || index >= objs.length) {
			return null;
		}

		Object value = objs[index];

		if (value != NOT_FOUND) {
			return (T) value;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getOrUpdate(int index, Supplier<T> producer) {
		if (index < 0) {
			throw new IllegalArgumentException("Illegal index: " + index);
		}

		Object value;
		if (objs == null || index >= objs.length) {
			value = NOT_FOUND;
		} else {
			value = objs[index];
		}

		if (value != NOT_FOUND) {
			return (T) value;
		}

		value = producer.get();
		put(index, (T) value);

		return (T) value;
	}

	private void ensureCapacity(int capacity) {
		if (objs != null && objs.length >= capacity) {
			return;
		}

		int newCapacity = tableSizeFor(capacity);

		if (newCapacity > MAXIMUM_CAPACITY) {
			throw new IndexOutOfBoundsException(newCapacity);
		}

		Object[] newArray = new Object[newCapacity];
		Arrays.fill(newArray, NOT_FOUND);

		if (objs != null) {
			System.arraycopy(objs, 0, newArray, 0, objs.length);
		}

		this.objs = newArray;
	}

	private static final int tableSizeFor(int cap) {
		int n = cap - 1;

		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;

		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}

}
