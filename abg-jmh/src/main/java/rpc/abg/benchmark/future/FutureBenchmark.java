package rpc.abg.benchmark.future;

import java.util.concurrent.CompletableFuture;
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

import rpc.abg.util.concurrent.FutureUtils;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class FutureBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object boxInteger1() {
		return Integer.valueOf(1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object completableFuture1() {
		return CompletableFuture.completedFuture(Integer.valueOf(1)).join();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object completableFutureWithTimeout() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		future.orTimeout(1, TimeUnit.SECONDS);

		future.complete(Integer.valueOf(1));

		return future.join();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object completableFutureWithFailover() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		CompletableFuture<Integer> futureWithFailover = FutureUtils.withFailover(//
				future, () -> CompletableFuture.completedFuture(Integer.valueOf(100)));

		future.complete(Integer.valueOf(1));

		return futureWithFailover.join();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object when() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		future.whenComplete((r, t) -> {

		});

		future.complete(Integer.valueOf(1));

		return future.join();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Object whenWhen() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		future.whenComplete((r, t) -> {

		}).whenComplete((r, t) -> {

		});

		future.complete(Integer.valueOf(1));

		return future.join();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(FutureBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(2)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

		// new InvokerBenchmark();
	}

}
