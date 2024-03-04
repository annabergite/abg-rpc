package rpc.abg.benchmark.client;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
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
import org.openjdk.jmh.runner.options.TimeValue;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.client.abgClient;

@State(Scope.Benchmark)
public class RpcClientBenchmark extends AbstractClient {

	public static final int CONCURRENCY = 32;

	private final abgClient client;
	private final UserService userService;

	public RpcClientBenchmark() {

		// client = new abgClient();
		// client.addConnect("shop", "auth", new HostPort("127.0.0.1", 8080));
		client = new abgClient("abg-client.conf");

		try {
			/*
			 * client.addFirst(new RpcClientFilter() {
			 * 
			 * @Override public boolean onSend(Request request) { try { Tracer tracer =
			 * TracerContext.nextTracer();
			 * 
			 * if (tracer != null) { RemoteContext.getClientAddress().toString();
			 * RemoteContext.getServerAddress().toString();
			 * RemoteContext.getServiceMethodName();
			 * 
			 * request.setTracer(tracer); } } catch (Exception e) { e.printStackTrace(); }
			 * 
			 * return true; }
			 * 
			 * @Override public void onRecive(Request request, Response response) { }
			 * 
			 * @Override public void onError(Request request, Response response, Throwable
			 * throwable) { } });
			 */

			client.register(UserService.class);
			userService = client.getService(UserService.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected UserService getUserService() {
		return userService;
	}

	@TearDown
	public void close() throws IOException {
		client.close();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object existUser() throws Exception {
		return super.existUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object createUser() throws Exception {
		return super.createUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object getUser() throws Exception {
		return super.getUser();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@Override
	public Object listUser() throws Exception {
		return super.listUser();
	}

	public static void main(String[] args) throws Exception {
		ResourceLeakDetector.setLevel(Level.DISABLED);
		// CtClass.debugDump = "d:/debugDump";

		RpcClientBenchmark clientBenchmark = new RpcClientBenchmark();
		// System.out.println(clientBenchmark.existUser());
		// System.out.println(clientBenchmark.getUser());
		// System.out.println(clientBenchmark.listUser());
		// System.out.println(clientBenchmark.createUser());

		// System.exit(1);

		ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(256);
		RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(256, 256, Long.MAX_VALUE, TimeUnit.DAYS, workQueue,
				rejectedExecutionHandler);

		Instant last = Instant.now();
		for (int i = 0; i < 1/*Long.MAX_VALUE*/; i++) {

			executor.submit(() -> {
				try {
					clientBenchmark.createUser();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			// clientBenchmark.getUser();

			if (i % 1_000_000 == 0) {
				Instant now = Instant.now();
				Duration duration = Duration.between(last, now);
				last = now;

				System.out.println(i + " \t " + duration);

				if (i == 2_000_000_000) {
					executor.shutdownNow();
					clientBenchmark.close();

					System.exit(1);
				}
			}
		}

		clientBenchmark.close();

		Options opt = new OptionsBuilder()//
				.include(RpcClientBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.warmupTime(TimeValue.seconds(10))//
				.measurementIterations(3)//
				.measurementTime(TimeValue.seconds(10))//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
