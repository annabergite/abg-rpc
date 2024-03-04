package rpc.abg.benchmark.map;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jctools.maps.NonBlockingIdentityHashMap;
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

import com.esotericsoftware.kryo.util.ObjectMap;

@State(Scope.Benchmark)
public class IdentityHashMapBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final IdentityHashMap<Class<?>, Integer> identityHashMap = new IdentityHashMap<>(512);
	private final HashMap<Class<?>, Integer> classHashMap = new HashMap<>(512);
	private final HashMap<Integer, Integer> idHashMap = new HashMap<>(512);
	private final ConcurrentHashMap<Class<?>, Integer> concurrentMap = new ConcurrentHashMap<>(512);
	private final ObjectMap<Class<?>, Integer> kryoObjectMap = new ObjectMap<>(512, 0.5F);
	private final NonBlockingIdentityHashMap<Class<?>, Integer>  nonBlockingIdentityHashMap = new NonBlockingIdentityHashMap<>();

	public IdentityHashMapBenchmark() {
		identityHashMap.put(int.class, 1);
		identityHashMap.put(Integer.class, 1);
		identityHashMap.put(long.class, 1);
		identityHashMap.put(Long.class, 1);
		identityHashMap.put(boolean.class, 1);
		identityHashMap.put(Boolean.class, 1);
		identityHashMap.put(double.class, 1);
		identityHashMap.put(Double.class, 1);
		identityHashMap.put(float.class, 1);
		identityHashMap.put(Float.class, 1);
		identityHashMap.put(short.class, 1);
		identityHashMap.put(Short.class, 1);
		identityHashMap.put(byte.class, 1);
		identityHashMap.put(Byte.class, 1);
		identityHashMap.put(char.class, 1);
		identityHashMap.put(Character.class, 1);
		identityHashMap.put(String.class, 1);
		identityHashMap.put(CharSequence.class, 1);
		identityHashMap.put(BigInteger.class, 1);
		identityHashMap.put(BigDecimal.class, 1);

		identityHashMap.forEach((key, value) -> {
			classHashMap.put(key, value);
			concurrentMap.put(key, value);
			idHashMap.put(System.identityHashCode(key), value);
			kryoObjectMap.put(key, value);
			nonBlockingIdentityHashMap.put(key, value);
		});
	}

	private static Integer VALUE = Integer.valueOf(1);

	private static Integer getValue(Class<?> clazz) {
		if (clazz == int.class) {
			return VALUE;
		}
		if (clazz == Integer.class) {
			return VALUE;
		}
		if (clazz == long.class) {
			return VALUE;
		}
		if (clazz == Long.class) {
			return VALUE;
		}
		if (clazz == boolean.class) {
			return VALUE;
		}
		if (clazz == Boolean.class) {
			return VALUE;
		}
		if (clazz == double.class) {
			return VALUE;
		}
		if (clazz == Double.class) {
			return VALUE;
		}
		if (clazz == float.class) {
			return VALUE;
		}
		if (clazz == Float.class) {
			return VALUE;
		}
		if (clazz == short.class) {
			return VALUE;
		}
		if (clazz == Short.class) {
			return VALUE;
		}
		if (clazz == byte.class) {
			return VALUE;
		}
		if (clazz == Byte.class) {
			return VALUE;
		}
		if (clazz == char.class) {
			return VALUE;
		}
		if (clazz == Character.class) {
			return VALUE;
		}
		if (clazz == String.class) {
			return VALUE;
		}
		if (clazz == CharSequence.class) {
			return VALUE;
		}
		if (clazz == BigInteger.class) {
			return VALUE;
		}
		if (clazz == BigDecimal.class) {
			return VALUE;
		}
		return VALUE;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer identityHashMap() {
		return identityHashMap.get(int.class);
	}
	
	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer nonBlockingIdentityHashMap() {
		return nonBlockingIdentityHashMap.get(int.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer ifFirst() {
		return getValue(int.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer ifLast() {
		return getValue(BigDecimal.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer classHashMap() {
		return classHashMap.get(int.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer idHashMap() {
		return idHashMap.get(System.identityHashCode(int.class));
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer concurrentMap() {
		return concurrentMap.get(int.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Integer kryoObjectMap() {
		return kryoObjectMap.get(int.class);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(IdentityHashMapBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
