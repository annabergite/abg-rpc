package rpc.abg.benchmark.uuid;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import rpc.abg.util.SystemClock;
import rpc.abg.util.concurrent.ConcurrentIntegerSequencer;
import rpc.abg.util.concurrent.ConcurrentLongSequencer;
import rpc.abg.util.uuid.ObjectId;
import rpc.abg.util.uuid.ObjectId128;
import rpc.abg.util.uuid.RandomId;
import rpc.abg.util.uuid.RandomId128;

@State(Scope.Benchmark)
public class UUIDBenchmark {

	private final AtomicInteger atomicInteger = new AtomicInteger(new SecureRandom().nextInt());
	private final LongAdder longAdder = new LongAdder();
	private final ConcurrentIntegerSequencer intSequencer = new ConcurrentIntegerSequencer(
			new SecureRandom().nextInt());
	private final ConcurrentLongSequencer longSequencer = new ConcurrentLongSequencer(new SecureRandom().nextInt());

	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public void _do_nothing() {
	// }

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String uuidString() throws Exception {
		return UUID.randomUUID().toString();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long currentTimeMillis() throws Exception {
		return System.currentTimeMillis();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long systemClockMills() throws Exception {
		return SystemClock.fast().mills();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long systemClockSeconds() throws Exception {
		return SystemClock.fast().seconds();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long nanoTime() throws Exception {
		return System.nanoTime();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Instant nowInstant() throws Exception {
		return Instant.now();
	}

	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public int _nextIntByIntegerSequence() throws Exception {
	// return intSequencer.next();
	// }
	//
	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public int nextIntByAtomicInteger() throws Exception {
	// return atomicInteger.getAndIncrement();
	// }
	//
	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public long nextLongByLongSequence() throws Exception {
	// return longSequencer.next();
	// }
	//
	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public long nextLongByRandom() throws Exception {
	// return ThreadLocalRandom.current().nextLong();
	// }
	//
	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public long nextLongByLongAdder() throws Exception {
	// longAdder.increment();
	// return longAdder.longValue();
	// }
	//
	// @Benchmark
	// @BenchmarkMode({ Mode.Throughput })
	// @OutputTimeUnit(TimeUnit.MICROSECONDS)
	// public void incrementLongAdder() throws Exception {
	// longAdder.increment();
	// }

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public RandomId newRandomId() throws Exception {
		return RandomId.next();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String newRandomIdHexString() throws Exception {
		return RandomId.next().toHexString();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public RandomId128 newRandomId128() throws Exception {
		return RandomId128.next();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String newRandomId128HexString() throws Exception {
		return RandomId128.next().toHexString();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ObjectId newObjectId() throws Exception {
		return ObjectId.next();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String newObjectIdHexString() throws Exception {
		return ObjectId.next().toHexString();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ObjectId128 newObjectId128() throws Exception {
		return ObjectId128.next();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String newObjectId128HexString() throws Exception {
		return ObjectId128.next().toHexString();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Object newObject() throws Exception {
		return new Object();
	}

	public static void main(String[] args) throws Exception {
		Instant instant = Instant.now();

		long currentMicros = instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000;

		System.out.println(currentMicros);
		System.out.println(System.currentTimeMillis());
		System.out.println(Long.MAX_VALUE);

		// System.exit(0);

		Options opt = new OptionsBuilder()//
				.include(UUIDBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(3)//
				.threads(4)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
