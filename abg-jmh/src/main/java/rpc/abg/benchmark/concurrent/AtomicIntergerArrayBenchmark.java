package rpc.abg.benchmark.concurrent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;

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

import rpc.abg.util.concurrent.AtomicMuiltInteger;

@State(Scope.Benchmark)
public class AtomicIntergerArrayBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors() * 4;

	private final int length = 4;
	private final AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(length);
	private final AtomicMuiltInteger atomicMuiltInteger = new AtomicMuiltInteger(length);
	private final AtomicMuiltInteger1 atomicMuiltInteger1 = new AtomicMuiltInteger1(length);
	private final AtomicMuiltInteger2 atomicMuiltInteger2 = new AtomicMuiltInteger2(length);

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _getIndex() {
		ThreadLocalRandom.current().nextInt(length);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void atomicIntegerArray() {
		int index = ThreadLocalRandom.current().nextInt(length);
		int value = atomicIntegerArray.get(index);
		atomicIntegerArray.set(index, value + 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void atomicMuiltInteger() {
		int index = ThreadLocalRandom.current().nextInt(length);
		int value = atomicMuiltInteger.get(index);
		atomicMuiltInteger.set(index, value + 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void atomicMuiltInteger1() {
		int index = ThreadLocalRandom.current().nextInt(length);
		int value = atomicMuiltInteger1.get(index);
		atomicMuiltInteger2.set(index, value + 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void atomicMuiltInteger2() {
		int index = ThreadLocalRandom.current().nextInt(length);
		int value = atomicMuiltInteger2.get(index);
		atomicMuiltInteger2.set(index, value + 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int sumByAtomicIntegerArray() {
		atomicIntegerArray.set(1, 1);

		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum = atomicIntegerArray.get(i);
		}

		return sum;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int sumByAtomicMuiltInteger() {
		atomicMuiltInteger.set(1, 1);
		return atomicMuiltInteger.sum();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int sumByAtomicMuiltInteger1() {
		atomicMuiltInteger1.set(1, 1);
		return atomicMuiltInteger1.sum();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int sumByAtomicMuiltInteger2() {
		atomicMuiltInteger2.set(1, 1);
		return atomicMuiltInteger2.sum();
	}

	public static void main(String[] args) throws RunnerException {
		AtomicMuiltInteger atomicMuiltInteger = new AtomicMuiltInteger(4);

		if ("true" == "true") {
			System.out.println(atomicMuiltInteger.sum());

			for (int i = 0; i < 4; i++) {
				System.out.println(atomicMuiltInteger.get(i));
			}

			for (int i = 0; i < 4; i++) {
				atomicMuiltInteger.set(i, i);
			}

			for (int i = 0; i < 4; i++) {
				System.out.println(atomicMuiltInteger.get(i));
			}

			System.out.println(atomicMuiltInteger.sum());
		}

		Options opt = new OptionsBuilder()//
				.include(AtomicIntergerArrayBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
