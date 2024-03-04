package rpc.abg.benchmark.pool;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import cn.nextop.lite.pool.Pool;
import cn.nextop.lite.pool.PoolBuilder;

@State(Scope.Benchmark)
public class ObjectPoolBenchmark {
	public static final int NCPU = Runtime.getRuntime().availableProcessors();
	public static final int CONCURRENCY = NCPU * 2;

	private final Supplier<Object> producer = () -> new Object();
	private final LockObjectPool<Object> lockObjectPool = new LockObjectPool<>(NCPU, producer);
	private final ViberObjectPool<Object> viberObjectPool = new ViberObjectPool<>(NCPU, producer);
	private final ConcurrentObjectPool<Object> concurrentObjectPool = new ConcurrentObjectPool<>(NCPU, producer);
	private final ConcurrentObjectPool2<Object> concurrentObjectPool2 = new ConcurrentObjectPool2<>(NCPU, producer);

	Pool<Object> litePool = new PoolBuilder<Object>()//
			.local(true) // using thread local
			.supplier(producer) //
			.minimum(NCPU) //
			.maximum(NCPU) //
			.build("object pool");

	@Setup
	public void init() {
		litePool.start();
	}

	@TearDown
	public void close() {

		try {
			lockObjectPool.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			viberObjectPool.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			concurrentObjectPool.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			concurrentObjectPool2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			litePool.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Object newObject() {
		return new Object();
	}

	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public void blazeObjectPool() throws Exception {
	// PoolableObject<Object> poolableObject = blazeObjectPool.claim();
	// poolableObject.release();
	// }

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void lockObjectPool() {
		Object obj = lockObjectPool.borrow();
		lockObjectPool.release(obj);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void viberObjectPool() {
		Object obj = viberObjectPool.borrow();
		viberObjectPool.release(obj);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void concurrentObjectPool() {
		Object obj = concurrentObjectPool.borrow();
		concurrentObjectPool.release(obj);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _concurrentObjectPool2() {
		Object obj = concurrentObjectPool2.borrow();
		concurrentObjectPool2.release(obj);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void litePool() {
		Object obj = litePool.acquire();
		litePool.release(obj);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(ObjectPoolBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
