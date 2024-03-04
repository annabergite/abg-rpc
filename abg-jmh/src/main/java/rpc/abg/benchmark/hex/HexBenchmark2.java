package rpc.abg.benchmark.hex;

import java.util.concurrent.TimeUnit;

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

import rpc.abg.util.HexUtils;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class HexBenchmark2 {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String int_HexUtils_toHex() {
		return HexUtils.toHex(Integer.MAX_VALUE);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String int_Integer_toHexString() {
		return Integer.toHexString(Integer.MAX_VALUE);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String long_HexUtils_toHex() {
		return HexUtils.toHex(Long.MAX_VALUE);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String long_Long_toHexString() {
		return Long.toHexString(Long.MAX_VALUE);
	}

	public static void main(String[] args) throws RunnerException {

		Options opt = new OptionsBuilder()//
				.include(HexBenchmark2.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
