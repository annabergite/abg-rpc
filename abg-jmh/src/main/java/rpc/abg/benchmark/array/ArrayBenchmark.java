package rpc.abg.benchmark.array;

import java.nio.ByteBuffer;
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

import rpc.abg.util.UnsafeUtils;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Thread)
public class ArrayBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final int length = 1024;
	private byte[] bytes = _newBytes();
	private byte[] bytes2 = _newBytes();
	private ByteBuffer byteBuffe = allocateByteBuffer();
	private ByteBuffer directByteBuffe = allocateDirectByteBuffer();

	public ArrayBenchmark() {
		byteBuffe.flip();
		directByteBuffe.flip();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte[] _newBytes() {
		return new byte[1024];
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ByteBuffer allocateByteBuffer() {
		return ByteBuffer.allocate(length);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ByteBuffer allocateDirectByteBuffer() {
		return ByteBuffer.allocateDirect(length);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte loopReadByteBytes() {
		byte value = 0;

		for (int i = 0; i < length; i++) {
			value = bytes[i];
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte loopReadByteBytesForeach() {
		byte value = 0;

		for (byte b : bytes) {
			value = b;
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte loopReadByteByteBuffer() {
		byte value = 0;

		byteBuffe.position(0);
		while (byteBuffe.hasRemaining()) {
			value = byteBuffe.get();
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte loopReadByteDirectByteBuffer() {
		byte value = 0;

		directByteBuffe.position(0);
		while (directByteBuffe.hasRemaining()) {
			value = directByteBuffe.get();
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long loopReadLongByteBuffer() {
		long value = 0;

		byteBuffe.position(0);
		while (byteBuffe.hasRemaining()) {
			value = byteBuffe.getLong();
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public long loopReadLongDirectByteBuffer() {
		long value = 0;

		directByteBuffe.position(0);
		while (directByteBuffe.hasRemaining()) {
			value = directByteBuffe.getLong();
		}

		return value;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void loopSetBytes() {
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) i;
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void unsafeCopyMemorySetByteBytes() {
		UnsafeUtils.unsafe().copyMemory(bytes, 0, bytes2, 0, length);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void z_arraycopyByteBytes() {
		System.arraycopy(bytes, 0, bytes2, 0, length);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void loopSetByteBuffer() {
		byteBuffe.clear();

		for (int i = 0; i < length; i++) {
			byteBuffe.put((byte) i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void loopSetDirectByteBuffer() {
		directByteBuffe.clear();

		for (int i = 0; i < length; i++) {
			directByteBuffe.put((byte) i);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void batchSetByteBuffer() {
		byteBuffe.clear();
		byteBuffe.put(bytes);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void batchSetDirectByteBuffer() {
		directByteBuffe.clear();
		directByteBuffe.put(bytes);
	}

	public static void main(String[] args) throws RunnerException {

		Options opt = new OptionsBuilder()//
				.include(ArrayBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
