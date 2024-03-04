package rpc.abg.benchmark.map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jctools.maps.NonBlockingHashMapLong;
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

import rpc.abg.util.concurrent.ConcurrentIntToObjectArrayMap;

@State(Scope.Benchmark)
public class ConcurrentIntArrayMapBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final ConcurrentIntToObjectArrayMap<Integer> intArrayMap = new ConcurrentIntToObjectArrayMap<>();
	private final ConcurrentHashMap<Integer, Integer> concurrentMap = new ConcurrentHashMap<>(1024 * 4 / 3);
	private final NonBlockingHashMapLong<Integer> nonBlockingHashMapLong = new NonBlockingHashMapLong<>();

	public ConcurrentIntArrayMapBenchmark() {
		for (int i = 0; i < 1024; i++) {
			intArrayMap.put(i, i);
			concurrentMap.put(i, i);
			nonBlockingHashMapLong.put(i, i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int intArrayMap() {
		return intArrayMap.get(512);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int getOrUpdateIntArrayMap() {
		return intArrayMap.getOrUpdate(512, () -> 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int concurrentMap() {
		return concurrentMap.get(512);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int nonBlockingHashMapLong() {
		return nonBlockingHashMapLong.get(512);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(ConcurrentIntArrayMapBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
