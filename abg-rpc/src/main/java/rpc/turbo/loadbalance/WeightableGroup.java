package rpc.abg.loadbalance;

import java.util.ArrayList;
import java.util.List;

import rpc.abg.util.MathUtils;

/**
 * 不可变，线程安全
 * 
 * @author Annabergite
 *
 * @param <T>
 *            必须为Weightable子类
 */
public final class WeightableGroup<T extends Weightable> {
	private final ArrayList<T> weightables;
	private final int[] weightLadder;
	private final int weightSum;
	private final boolean fastMode;

	public WeightableGroup(final List<T> weightables) {
		ArrayList<T> weightableList = new ArrayList<>(weightables.size());

		for (T t : weightables) {
			if (t == null) {
				continue;
			}

			if (t.weight() < 1) {
				continue;
			}

			weightableList.add(t);
		}

		if (weightableList.size() == 0) {
			this.weightables = weightableList;
			this.weightLadder = new int[0];
			this.weightSum = 0;

			this.fastMode = true;

			return;
		}

		if (weightableList.size() == 1) {
			this.weightables = weightableList;
			this.weightLadder = new int[1];
			this.weightSum = 1;

			this.fastMode = true;

			return;
		}

		int[] weights = new int[weightableList.size()];

		for (int i = 0; i < weightableList.size(); i++) {
			weights[i] = weightableList.get(i).weight();
		}

		int gcd = MathUtils.gcd(weights);

		if (gcd > 1) {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = weights[i] / gcd;
			}
		}

		boolean isFastMode = true;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] != 1) {
				isFastMode = false;
				break;
			}
		}

		this.fastMode = isFastMode;

		for (int i = 1; i < weights.length; i++) {
			weights[i] = weights[i] + weights[i - 1];
		}

		for (int i = 0; i < weights.length; i++) {
			weights[i] -= 1;// [0,1) [1,2) 前开后闭
		}

		this.weightables = weightableList;
		this.weightLadder = weights;
		this.weightSum = weights[weights.length - 1];
	}

	/**
	 * 获取weight之和，配合{@link #get(int)}使用<br>
	 * 
	 * <pre>
	 * int seed = rendom.nextInt(sum());
	 * T t = get(seed);
	 * </pre>
	 * 
	 * @return
	 */
	public final int sum() {
		return weightSum;
	}

	/**
	 * 根据种子获取一个元素, 配合 {@link #sum()}使用<br>
	 * 
	 * <pre>
	 * int seed = rendom.nextInt(sum());
	 * T t = get(seed);
	 * </pre>
	 * 
	 * @param seed
	 * @return
	 */
	public final T get(int seed) {
		if (weightSum == 0) {
			return null;
		}

		if (weightSum == 1) {
			return weightables.get(0);
		}

		if (seed < 0) {
			seed = -seed;
		}

		if (seed > weightSum) {
			seed = seed % (weightSum + 1);
		}

		if (fastMode) {
			return weightables.get(seed);
		}

		int index = binarySearch(weightLadder, seed);

		return weightables.get(index);
	}

	private static final int binarySearch(int[] values, int key) {
		int low = 0;
		int high = values.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = values[mid];

			if (midVal < key) {
				low = mid + 1;
			} else if (midVal > key) {
				high = mid - 1;
			} else {
				return mid; // key found
			}
		}

		return low; // key not found.
	}

	public static void main(String[] args) {
		int[] weightLadder = { 1, 5, 8, 15 };
		int index = binarySearch(weightLadder, 15);
		System.out.println(index);
	}

}
