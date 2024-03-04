package rpc.abg.benchmark.invoke;

import java.lang.reflect.Method;
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

import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.JavassistInvoker;
import rpc.abg.invoke.MethodHandleInvoker;
import rpc.abg.invoke.ReflectInvoker;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class InvokerBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final IntegerService integerService;
	private final Method getIntegerMethod;
	private final Invoker<Integer> lightInvoker;
	private final Invoker<Integer> javassistInvoker;
	private final Invoker<Integer> reflectInvoker;
	private final Invoker<Integer> methodHandleInvoker;

	public InvokerBenchmark() {

		try {
			this.integerService = new IntegerServiceImpl();
			this.getIntegerMethod = IntegerService.class.getDeclaredMethod("getValue");
			this.lightInvoker = (Object... params) -> integerService.getValue();
			this.javassistInvoker = new JavassistInvoker<>(1, integerService, IntegerService.class, getIntegerMethod);
			this.reflectInvoker = new ReflectInvoker<>(1, integerService, IntegerService.class, getIntegerMethod);
			this.methodHandleInvoker = new MethodHandleInvoker<>(1, integerService, IntegerService.class,
					getIntegerMethod);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer directInvoke() {
		return integerService.getValue();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer lightInvoke() {
		return lightInvoker.invoke();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer javassistInvoke() {
		return javassistInvoker.invoke();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer reflectInvoke() {
		return reflectInvoker.invoke();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer methodHandleInvoke() {
		return methodHandleInvoker.invoke();
	}

	public static void main(String[] args) throws RunnerException {

		InvokerBenchmark invokerBenchmark = new InvokerBenchmark();
		System.out.println(invokerBenchmark.methodHandleInvoke());

		Options opt = new OptionsBuilder()//
				.include(InvokerBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
