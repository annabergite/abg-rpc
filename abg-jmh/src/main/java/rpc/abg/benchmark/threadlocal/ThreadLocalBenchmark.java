package rpc.abg.benchmark.threadlocal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.internal.InternalThreadLocalMap;
import rpc.abg.util.concurrent.AttachmentThread;
import rpc.abg.util.concurrent.AttachmentThreadUtils;
import rpc.abg.util.concurrent.ConcurrentIntToObjectArrayMap;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class ThreadLocalBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final Integer value = 100;
	private final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
	private final ConcurrentIntToObjectArrayMap<Integer> arrayMap = new ConcurrentIntToObjectArrayMap<>(1024);
	private final ConcurrentHashMap<Long, Integer> threadIdMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Thread, Integer> threadMap = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<Integer> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
	private final Holder holder = new Holder(new Holder(new ConcurrentIntToObjectArrayMap<>()));
	private final Supplier<Integer> supplier = () -> value;
	private final FastThreadLocalThread fastThread = new FastThreadLocalThread();
	private final AttachmentThread attachmentThread = new AttachmentThread();

	public ThreadLocalBenchmark() {
		fastThread.setThreadLocalMap(InternalThreadLocalMap.get());
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer directGet() {
		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer threadLocal() {
		Integer value = threadLocal.get();

		if (value != null) {
			return value;
		}

		value = 100;
		threadLocal.set(value);

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	@SuppressWarnings("unchecked")
	public Integer fastThreadWithArrayMap() {
		Object obj = fastThread.threadLocalMap().indexedVariable(1024);
		ConcurrentIntToObjectArrayMap<Integer> map;

		if (obj != InternalThreadLocalMap.UNSET) {
			map = (ConcurrentIntToObjectArrayMap<Integer>) obj;
			return map.getOrUpdate(1024, () -> value);
		}

		map = new ConcurrentIntToObjectArrayMap<>();
		fastThread.threadLocalMap().setIndexedVariable(1024, map);

		return map.getOrUpdate(1024, () -> value);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer fastThread() {
		Object obj = fastThread.threadLocalMap().indexedVariable(256);

		if (obj != InternalThreadLocalMap.UNSET) {
			return (Integer) obj;
		}

		Integer value = 100;
		fastThread.threadLocalMap().setIndexedVariable(256, value);

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer attachmentThread() {
		return attachmentThread.getOrUpdate(1, supplier);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer attachmentThreadUtils() {
		return AttachmentThreadUtils.getOrUpdate(1, supplier);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer arrayMap() {
		int key = (int) Thread.currentThread().getId();
		return arrayMap.getOrUpdate(key, () -> 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer arrayMapConstantProducer() {
		int key = (int) Thread.currentThread().getId();
		return arrayMap.getOrUpdate(key, supplier);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer threadMap() {
		Thread key = Thread.currentThread();
		Integer value = threadMap.get(key);

		if (value != null) {
			return value;
		}

		value = 100;
		threadMap.put(key, value);

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer threadIdMap() {
		Long key = Thread.currentThread().getId();
		Integer value = threadIdMap.get(key);

		if (value != null) {
			return value;
		}

		value = 100;
		threadIdMap.put(key, value);

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer concurrentLinkedQueue() {
		Integer value = concurrentLinkedQueue.poll();

		if (value == null) {
			value = 100;
		}

		concurrentLinkedQueue.add(value);

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	@SuppressWarnings("unchecked")
	public Integer getByHolder() {
		return ((ConcurrentIntToObjectArrayMap<Integer>) ((Holder) holder.obj).obj).getOrUpdate(1, () -> 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Thread currentThread() {
		return Thread.currentThread();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(ThreadLocalBenchmark.class.getName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}

class Holder {
	public final Object obj;

	public Holder(Object obj) {
		this.obj = obj;
	}

}