package rpc.abg.benchmark.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceJsonHttpClientImpl;

@State(Scope.Benchmark)
public class RestClientBenchmark extends AbstractClient {

	public static final int CONCURRENCY = 32;

	private final UserServiceJsonHttpClientImpl userService;

	public RestClientBenchmark() {
		userService = new UserServiceJsonHttpClientImpl(8);
	}

	@Override
	protected UserService getUserService() {
		return userService;
	}

	@TearDown
	public void close() throws IOException {
		userService.close();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput, Mode.AverageTime, Mode.SampleTime })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object existUser() throws Exception {
		return super.existUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput, Mode.AverageTime, Mode.SampleTime })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object createUser() throws Exception {
		return super.createUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput, Mode.AverageTime, Mode.SampleTime })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object getUser() throws Exception {
		return super.getUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput, Mode.AverageTime, Mode.SampleTime })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object listUser() throws Exception {
		return super.listUser();
	}

	public static void main(String[] args) throws Exception {
		ResourceLeakDetector.setLevel(Level.DISABLED);
		// CtClass.debugDump = "d:/debugDump";

		// RestClientBenchmark clientBenchmark = new RestClientBenchmark();
		// System.out.println(clientBenchmark.createUser());
		// clientBenchmark.close();

		Options opt = new OptionsBuilder()//
				.include(RestClientBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
