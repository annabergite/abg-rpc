package rpc.abg.benchmark.ip;

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

import rpc.abg.util.IPUtils;

@State(Scope.Benchmark)
public class IPUtilsBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final String ip = "192.168.1.18";

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int ipToInt() {
		return IPUtils.ipToInt(ip);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long ipToLong() {
		return IPUtils.ipToLong(ip);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(IPUtilsBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
