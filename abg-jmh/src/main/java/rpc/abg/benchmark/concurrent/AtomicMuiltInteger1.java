package rpc.abg.benchmark.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

/**
 * 原子性操作多个int
 * 
 * @author Annabergite
 *
 */
public class AtomicMuiltInteger1 {

	private static final VarHandle AA = MethodHandles.arrayElementVarHandle(int[].class);

	private final int[] array;
	private final int count;

	/**
	 * 原子性操作多个int，初始值为0
	 * 
	 * @param count
	 *            数量
	 */
	public AtomicMuiltInteger1(int count) {
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
	public AtomicMuiltInteger1(int count, int initialValue) {
		if (count < 1) {// 必须大于0
			throw new IllegalArgumentException("Illegal count: " + count);
		}

		this.count = count;
		array = new int[(count + 1) * 17];
		Arrays.fill(array, initialValue);
	}

	/**
	 * 递增并获取该位置的值
	 * 
	 * @param index
	 * @return
	 */
	public int incrementAndGet(int index) {
		return (int) AA.getAndAdd(array, offset(index), 1) + 1;
	}

	/**
	 * 增加并获取该位置的值
	 * 
	 * @param index
	 * @param delta
	 * @return
	 */
	public int addAndGet(int index, int delta) {
		return (int) AA.getAndAdd(array, offset(index), delta) + delta;

	}

	/**
	 * 某个位置的值
	 * 
	 * @param index
	 * @return
	 */
	public int get(int index) {
		return (int) AA.get(array, offset(index));
	}

	/**
	 * 所有位置的和
	 * 
	 * @return
	 */
	public int sum() {
		int sum = 0;

		for (int i = 0; i < count; i++) {
			sum += get(i);
		}

		return sum;
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
		AA.setOpaque(array, offset(index), value);
	}

	private static final int offset(int index) {
		int offset = index + 1;
		return (offset << 4);
	}

	public static void main(String[] args) {
		for (int i = 0; i < 10; i++) {
			System.out.println(offset(i));
		}

		AtomicMuiltInteger1 atomicMuiltInteger = new AtomicMuiltInteger1(4);

		System.out.println(atomicMuiltInteger.sum());
		System.out.println();

		for (int i = 0; i < 4; i++) {
			System.out.println(atomicMuiltInteger.get(i));
		}

		System.out.println();

		for (int i = 0; i < 4; i++) {
			atomicMuiltInteger.set(i, i + 1);
		}

		System.out.println();

		for (int i = 0; i < 4; i++) {
			System.out.println(atomicMuiltInteger.get(i));
		}

		System.out.println();

		System.out.println(atomicMuiltInteger.sum());
	}
}
