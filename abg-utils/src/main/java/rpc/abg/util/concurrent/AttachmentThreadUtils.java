package rpc.abg.util.concurrent;

import java.util.function.Supplier;

import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.InternalThreadLocalMap;
import rpc.abg.util.IntToObjectArrayMap;

/**
 * 当线程为AttachmentThread、FastThreadLocalThread时能达到ThreadLocal 1.5x的性能；<br>
 * 当线程不是上述两种，并且线程id小于16k时，可获得跟ThreadLocal相同的性能；<br>
 * 当线程不满足上述两种情况，将只能达到ThreadLocal 0.7x的性能；<br>
 * 请谨慎使用，小心内存泄漏！
 * 
 * @author Annabergite
 *
 */
public final class AttachmentThreadUtils {

	private static ThreadLocal<IntToObjectArrayMap<Object>> SLOW_THREAD_LOCAL_HOLDER//
			= ThreadLocal.withInitial(() -> new IntToObjectArrayMap<>());

	private static final ConcurrentIntToObjectArrayMap<IntToObjectArrayMap<Object>> threadAttachmentMap//
			= new ConcurrentIntToObjectArrayMap<>(1024);

	/**
	 * 寻找下一个线程本地变量位置
	 * 
	 * @return
	 */
	public static int nextVarIndex() {
		return InternalThreadLocalMap.nextVariableIndex();
	}

	/**
	 * 
	 * @param index
	 *            通过 {@link #nextVarIndex()} 获得
	 * 
	 * @param producer
	 *            尽量使用final常量以获得更好的性能
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getOrUpdate(int index, Supplier<T> producer) {

		Thread currentThread = Thread.currentThread();

		if (currentThread instanceof FastThreadLocalThread) {// 很快，1.5x ThreadLocal性能

			FastThreadLocalThread fastThread = (FastThreadLocalThread) currentThread;
			InternalThreadLocalMap threadLocalMap = fastThread.threadLocalMap();

			if (threadLocalMap == null) {
				// 会自动赋值的
				threadLocalMap = InternalThreadLocalMap.get();
			}

			Object obj = threadLocalMap.indexedVariable(index);
			if (obj != InternalThreadLocalMap.UNSET) {
				return (T) obj;
			}

			obj = producer.get();
			threadLocalMap.setIndexedVariable(index, obj);

			return (T) obj;

		}

		if (currentThread instanceof AttachmentThread) {// 很快，1.5x ThreadLocal性能

			AttachmentThread attachmentThread = (AttachmentThread) currentThread;
			T result = attachmentThread.get(index);

			if (result != null) {
				return result;
			} else {
				return attachmentThread.getOrUpdate(index, producer);
			}

		}

		long currentThreadId = currentThread.getId();
		if (currentThreadId < 1024L * 16L) {// 跟直接使用ThreadLocal相同的性能

			IntToObjectArrayMap<Object> varMap = threadAttachmentMap.get((int) currentThreadId);

			if (varMap == null) {
				varMap = threadAttachmentMap// 会自动赋值的
						.getOrUpdate((int) currentThreadId, () -> new IntToObjectArrayMap<>());
			}

			Object obj = varMap.get(index);

			if (obj != null) {
				return (T) obj;
			} else {
				return (T) varMap.getOrUpdate(index, (Supplier<Object>) producer);
			}

		}

		{// 很慢，0.7x ThreadLocal性能

			IntToObjectArrayMap<Object> varMap = SLOW_THREAD_LOCAL_HOLDER.get();
			Object obj = varMap.get(index);

			if (obj != null) {
				return (T) obj;
			} else {
				return (T) varMap.getOrUpdate(index, (Supplier<Object>) producer);
			}
		}
	}

	/**
	 * 
	 * @param index
	 *            通过 {@link #nextVarIndex()} 获得
	 * 
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(int index) {

		Thread currentThread = Thread.currentThread();

		if (currentThread instanceof FastThreadLocalThread) {// 很快，1.5x ThreadLocal性能

			FastThreadLocalThread fastThread = (FastThreadLocalThread) currentThread;
			InternalThreadLocalMap threadLocalMap = fastThread.threadLocalMap();

			if (threadLocalMap == null) {
				// 会自动赋值的
				threadLocalMap = InternalThreadLocalMap.get();
			}

			Object obj = threadLocalMap.indexedVariable(index);
			if (obj != InternalThreadLocalMap.UNSET) {
				return (T) obj;
			} else {
				return null;
			}

		}

		if (currentThread instanceof AttachmentThread) {// 很快，1.5x ThreadLocal性能

			AttachmentThread attachmentThread = (AttachmentThread) currentThread;
			return attachmentThread.get(index);

		}

		long currentThreadId = currentThread.getId();
		if (currentThreadId < 1024L * 16L) {// 跟直接使用ThreadLocal相同的性能

			IntToObjectArrayMap<Object> varMap = threadAttachmentMap.get((int) currentThreadId);

			if (varMap == null) {
				varMap = threadAttachmentMap// 会自动赋值的
						.getOrUpdate((int) currentThreadId, () -> new IntToObjectArrayMap<>());
			}

			return (T) varMap.get(index);

		}

		{// 很慢，0.7x ThreadLocal性能

			IntToObjectArrayMap<Object> varMap = SLOW_THREAD_LOCAL_HOLDER.get();
			return (T) varMap.get(index);

		}
	}

	/**
	 * 
	 * @param index
	 *            通过 {@link #nextVarIndex()} 获得
	 * 
	 * @param value
	 * 
	 */
	public static <T> void put(int index, T value) {

		Thread currentThread = Thread.currentThread();

		if (currentThread instanceof FastThreadLocalThread) {// 很快，1.5x ThreadLocal性能

			FastThreadLocalThread fastThread = (FastThreadLocalThread) currentThread;
			InternalThreadLocalMap threadLocalMap = fastThread.threadLocalMap();

			if (threadLocalMap == null) {
				// 会自动赋值的
				threadLocalMap = InternalThreadLocalMap.get();
			}

			threadLocalMap.setIndexedVariable(index, value);
			return;

		}

		if (currentThread instanceof AttachmentThread) {// 很快，1.5x ThreadLocal性能

			AttachmentThread attachmentThread = (AttachmentThread) currentThread;
			attachmentThread.put(index, value);
			return;

		}

		long currentThreadId = currentThread.getId();
		if (currentThreadId < 1024L * 16L) {// 跟直接使用ThreadLocal相同的性能

			IntToObjectArrayMap<Object> varMap = threadAttachmentMap.get((int) currentThreadId);

			if (varMap == null) {
				varMap = threadAttachmentMap// 会自动赋值的
						.getOrUpdate((int) currentThreadId, () -> new IntToObjectArrayMap<>());
			}

			varMap.put(index, value);
			return;

		}

		{// 很慢，0.7x ThreadLocal性能

			IntToObjectArrayMap<Object> varMap = SLOW_THREAD_LOCAL_HOLDER.get();
			varMap.put(index, value);
			return;

		}
	}

}
