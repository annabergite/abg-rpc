package rpc.abg.util;

import java.util.Arrays;

import com.google.common.math.IntMath;

public class MathUtils {

	/**
	 * 最大公约数
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static int gcd(int a, int b) {
		if (a == b) {
			return a;
		}

		return IntMath.gcd(a, b);
	}

	/**
	 * 最大公约数
	 * 
	 * @param numbers
	 * @return
	 */
	public static int gcd(int[] numbers) {
		if (numbers == null || numbers.length == 0) {
			throw new IllegalArgumentException("numbers cannot be empty");
		}

		int length = numbers.length;

		if (length == 1) {
			return numbers[0];
		}

		int[] values = Arrays.copyOf(numbers, length);
		Arrays.sort(values, 0, length);

		int newLentgh = length;
		while (newLentgh > 1) {
			newLentgh = removeDuplicate(values, newLentgh);
			newLentgh = gcdMerge(values, newLentgh);
		}

		return values[0];
	}

	/**
	 * 两两合并
	 * 
	 * @param numbers
	 * @param length
	 * @return 新的长度
	 */
	private static int gcdMerge(int[] numbers, int length) {
		if (length == 1) {
			return 1;
		}

		int newIndex = 0;
		int index = 0;

		while (index < length - 1) {
			numbers[newIndex] = gcd(numbers[index], numbers[index + 1]);

			newIndex += 1;
			index += 2;
		}

		if (index == length - 1) {
			numbers[newIndex] = numbers[length - 1];
			newIndex += 1;
		}

		return newIndex;
	}

	/**
	 * 删除重复元素
	 * 
	 * @param numbers
	 *            已排序
	 * 
	 * @param length
	 *            只处理前length个
	 * 
	 * @return 不重复元素数量
	 */
	private static int removeDuplicate(int[] numbers, int length) {
		if (length == 1 || numbers.length == 1) {
			return 1;
		}

		int newIndex = 0;
		int newValue = numbers[0];
		int index = 1;

		while (index < length) {
			if (numbers[index] == newValue) {
				++index;
			} else {
				++newIndex;

				newValue = numbers[index];
				numbers[newIndex] = newValue;

				++index;
			}
		}

		return newIndex + 1;
	}

	public static void main(String[] args) {
		System.out.println(gcd(new int[] { 100, 10, 15, 1 }));

		System.out.println(gcd(new int[] { 100, 10, 15, 7 }));

		System.out.println(gcd(new int[] { 100, 10, 15 }));
	}

}
