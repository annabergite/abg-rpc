package rpc.abg.benchmark.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

import rpc.abg.protocol.Response;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class FutureHolderBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors() * 2;

	AtomicInteger sequencer = new AtomicInteger(0);

	ResponseFutureContainer1 futureContainer1 = new ResponseFutureContainer1();
	ResponseFutureContainer2 futureContainer2 = new ResponseFutureContainer2();
	ResponseFutureContainer3 futureContainer3 = new ResponseFutureContainer3();

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _completableFuture() {
		int requestId = sequencer.getAndIncrement();
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		Response response = new Response();
		response.setRequestId(requestId - 1000);

		completableFuture.complete(response);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void futureContainer1() {
		int requestId = sequencer.getAndIncrement();
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		futureContainer1.addFuture(requestId, completableFuture);

		Response response = new Response();
		response.setRequestId(requestId - 1000);

		futureContainer1.notifyResponse(response);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void futureContainer2() {
		int requestId = sequencer.getAndIncrement();
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		futureContainer2.addFuture(requestId, completableFuture);

		Response response = new Response();
		response.setRequestId(requestId - 1000);

		futureContainer2.notifyResponse(response);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void futureContainer3() {
		int requestId = sequencer.getAndIncrement();
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		futureContainer3.addFuture(requestId, completableFuture);

		Response response = new Response();
		response.setRequestId(requestId - 1000);

		futureContainer3.notifyResponse(response);
	}

	public static void main(String[] args) throws RunnerException {

		Options opt = new OptionsBuilder()//
				.include(FutureHolderBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

		// new InvokerBenchmark();
	}

}
