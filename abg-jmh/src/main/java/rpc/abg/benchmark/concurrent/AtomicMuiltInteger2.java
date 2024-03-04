package rpc.abg.benchmark.concurrent;

import static rpc.abg.util.UnsafeUtils.unsafe;

import java.util.Arrays;
import java.util.Objects;

/**
 * 原子性操作多个int
 * 
 * @author Annabergite
 *
 */
public class AtomicMuiltInteger2 {
	private static final int ABASE;
	private static final int ASHIFT;

	private final int[] array;
	private final int count;
	private final long sumIndex;

	/**
	 * 原子性操作多个int，初始值为0
	 * 
	 * @param count
	 *            数量
	 */
	public AtomicMuiltInteger2(int count) {
		this(count, 0);
	}

	/**
	 * 原子性操作多个int
	 * 
	 * @param count
	 *            数量
	 * @param initialValue
	 *            初始值
	 */
	public AtomicMuiltInteger2(int count, int initialValue) {
		if (count < 1) {// 必须大于0
			throw new IllegalArgumentException("Illegal count: " + count);
		}

		this.count = count;
		array = new int[(count + 3) << 4];
		sumIndex = offset(count);
		Arrays.fill(array, initialValue);
	}

	/**
	 * 递增并获取该位置的值
	 * 
	 * @param index
	 * @return
	 */
	public int incrementAndGet(int index) {
		Objects.checkIndex(index, count);

		unsafe().getAndAddInt(array, sumIndex, 1);
		return unsafe().getAndAddInt(array, offset(index), 1) + 1;
	}

	/**
	 * 增加并获取该位置的值
	 * 
	 * @param index
	 * @param delta
	 * @return
	 */
	public int addAndGet(int index, int delta) {
		Objects.checkIndex(index, count);

		unsafe().getAndAddInt(array, sumIndex, delta);
		return unsafe().getAndAddInt(array, offset(index), delta) + delta;
	}

	/**
	 * 某个位置的值
	 * 
	 * @param index
	 * @return
	 */
	public int get(int index) {
		Objects.checkIndex(index, count);
		return unsafe().getIntVolatile(array, offset(index));
	}

	/**
	 * 所有位置的和
	 * 
	 * @return
	 */
	public int sum() {
		return unsafe().getIntVolatile(array, sumIndex);
	}

	/**
	 * 重置某位置的值为0
	 * 
	 * @param index
	 */
	public void reset(int index) {
		set(index, 0);
	}

	/**
	 * 重置所有位置的值为0
	 */
	public void resetAll() {
		for (int i = 0; i < count; i++) {
			set(i, 0);
		}
	}

	/**
	 * 设置某位置的值
	 * 
	 * @param index
	 * @param value
	 */
	public void set(int index, int value) {
		Objects.checkIndex(index, count);

		long offset = offset(index);

		while (true) {
			int old = unsafe().getIntVolatile(array, offset);

			if (!unsafe().compareAndSwapInt(array, offset, old, value)) {
				continue;
			}

			int delta = value - old;
			unsafe().getAndAddInt(array, sumIndex, delta);

			return;
		}
	}

	private static final long offset(int index) {
		return (((long) index + 1L) << ASHIFT) + ABASE;
	}

	static {
		try {
			ABASE = unsafe().arrayBaseOffset(int[].class);

			int scale = unsafe().arrayIndexScale(int[].class);
			if ((scale & (scale - 1)) != 0) {
				throw new Error("array index scale not a power of two");
			}

			ASHIFT = 31 - Integer.numberOfLeadingZeros(scale) + 4;
		} catch (Exception e) {
			throw new Error(e);
		}
	}
}
