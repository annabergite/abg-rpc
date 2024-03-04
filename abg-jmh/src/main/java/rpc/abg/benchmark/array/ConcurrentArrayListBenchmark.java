package rpc.abg.benchmark.array;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import rpc.abg.util.concurrent.ConcurrentArrayList;

@State(Scope.Benchmark)
public class ConcurrentArrayListBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final ArrayList<Boolean> arrayList = new ArrayList<>();
	private final ConcurrentArrayList<Boolean> concurrentArrayList = new ConcurrentArrayList<>();
	private final CopyOnWriteArrayList<Boolean> copyOnWriteArrayList = new CopyOnWriteArrayList<>();

	public ConcurrentArrayListBenchmark() {
		for (int i = 0; i < 1024; i++) {
			arrayList.add(Boolean.TRUE);
			concurrentArrayList.add(Boolean.TRUE);
			copyOnWriteArrayList.add(Boolean.TRUE);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	@Threads(100)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Boolean getByArrayList() {
		return arrayList.get(512);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Boolean getByConcurrentArrayList() {
		return concurrentArrayList.get(512);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Boolean getByCopyOnWriteArrayList() {
		return copyOnWriteArrayList.get(512);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void newAndPutWithArrayList() {
		ArrayList<Boolean> arrayList = new ArrayList<>();

		for (int i = 0; i < 1024; i++) {
			arrayList.add(Boolean.TRUE);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void newAndPutWithConcurrentArrayList() {
		ConcurrentArrayList<Boolean> concurrentArrayList = new ConcurrentArrayList<>(1024);

		for (int i = 0; i < 1024; i++) {
			concurrentArrayList.add(Boolean.TRUE);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void newAndPutWithCopyOnWriteArrayList() {
		CopyOnWriteArrayList<Boolean> copyOnWriteArrayList = new CopyOnWriteArrayList<>();

		for (int i = 0; i < 1024; i++) {
			copyOnWriteArrayList.add(Boolean.TRUE);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(ConcurrentArrayListBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				//.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
