package rpc.abg.benchmark.map;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.agrona.collections.Int2IntHashMap;
import org.agrona.collections.Int2ObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
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

import com.esotericsoftware.kryo.util.IntMap;

import io.netty.util.collection.IntObjectHashMap;
import rpc.abg.util.IntToObjectArrayMap;

@State(Scope.Thread)
public class OpenAddressBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final IntToObjectArrayMap<Integer> intArrayMap = new IntToObjectArrayMap<>();
	private final HashMap<Integer, Integer> hashMap = new HashMap<>();
	private final IntObjectHashMap<Integer> nettyMap = new IntObjectHashMap<>();
	private final IntMap<Integer> kryoMap = new IntMap<>();
	private final Int2IntHashMap agronaInt2IntMap = new Int2IntHashMap(-1);
	private final Int2ObjectHashMap<Integer> agronaInt2ObjectMap = new Int2ObjectHashMap<>();
	private final IntIntHashMap eclipseIntIntHashMap = new IntIntHashMap();
	private final IntObjectHashMap<Integer> eclipseIntObjectHashMap = new IntObjectHashMap<>();

	public OpenAddressBenchmark() {
		for (int i = 0; i < 1024 * 64; i++) {
			Integer obj = Integer.valueOf(i);

			intArrayMap.put(i, i);
			hashMap.put(i, obj);
			nettyMap.put(i, obj);
			kryoMap.put(i, obj);
			agronaInt2IntMap.put(i, i);
			agronaInt2ObjectMap.put(i, obj);
			eclipseIntIntHashMap.put(i, obj);
			eclipseIntObjectHashMap.put(i, obj);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putIntArrayMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			intArrayMap.put(i, i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getIntArrayMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			intArrayMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			hashMap.put(i, Integer.valueOf(i));
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			hashMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putNettyMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			nettyMap.put(i, Integer.valueOf(i));
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getNettyMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			nettyMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putKryoMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			kryoMap.put(i, Integer.valueOf(i));
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getKryoMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			kryoMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putAgronaInt2IntMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			agronaInt2IntMap.put(i, i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putEclipseIntIntHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			eclipseIntIntHashMap.put(i, i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putEclipseIntObjectHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			eclipseIntObjectHashMap.put(i, Integer.valueOf(i));
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getAgronaInt2IntMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			agronaInt2IntMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void putAgronaInt2ObjectMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			agronaInt2ObjectMap.put(i, Integer.valueOf(i));
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getAgronaInt2ObjectMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			agronaInt2ObjectMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getEclipseIntIntHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			eclipseIntIntHashMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void getEclipseIntObjectHashMap() {
		for (int i = 0; i < 1024 * 64; i++) {
			eclipseIntObjectHashMap.get(i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void boxInt() {
		Integer value = null;

		for (int i = 0; i < 1024 * 64; i++) {
			value = Integer.valueOf(i);
		}

		value.intValue();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(OpenAddressBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
