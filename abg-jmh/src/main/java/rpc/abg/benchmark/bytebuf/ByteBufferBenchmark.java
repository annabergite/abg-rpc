package rpc.abg.benchmark.bytebuf;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import rpc.abg.util.UnsafeUtils;

@State(Scope.Thread)
public class ByteBufferBenchmark {

	final byte[] bytes = new byte[1024];
	final ByteBuffer heapByteBuffer = ByteBuffer.allocate(1024);
	final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(1024);
	final ByteBuf nettyByteBuf = PooledByteBufAllocator.DEFAULT.buffer(1024, 1024);
	final long OFFSET = UnsafeUtils.unsafe().allocateMemory(1024);

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToBytes() {
		for (int i = 0; i < 1024; i++) {
			bytes[i] = 1;
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToHeap() {
		for (int i = 0; i < 1024; i++) {
			heapByteBuffer.put(i, (byte) 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToDirect() {
		for (int i = 0; i < 1024; i++) {
			directByteBuffer.put(i, (byte) 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToNetty() {
		for (int i = 0; i < 1024; i++) {
			nettyByteBuf.setByte(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToDirectAndCopyToNetty() {
		for (int i = 0; i < 1024; i++) {
			directByteBuffer.put(i, (byte) 1);
		}

		nettyByteBuf.setBytes(0, directByteBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByteToUnsafe() {
		for (int i = 0; i < 1024; i++) {
			UnsafeUtils.unsafe().putByte(OFFSET + i, (byte) 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToBytes() {
		for (int i = 0; i < 1024; i += 4) {
			bytes[i] = 1;
			bytes[i + 1] = 1;
			bytes[i + 2] = 1;
			bytes[i + 3] = 1;
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToHeap() {
		for (int i = 0; i < 1024; i += 4) {
			heapByteBuffer.putInt(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToDirect() {
		for (int i = 0; i < 1024; i += 4) {
			directByteBuffer.putInt(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToNetty() {
		for (int i = 0; i < 1024; i += 4) {
			nettyByteBuf.setInt(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToDirectAndCopyToNetty() {
		for (int i = 0; i < 1024; i += 4) {
			directByteBuffer.putInt(i, 1);
		}

		nettyByteBuf.setBytes(0, directByteBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeIntToUnsafe() {
		for (int i = 0; i < 1024; i += 4) {
			UnsafeUtils.unsafe().putInt(OFFSET + i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToBytes() {
		for (int i = 0; i < 1024; i += 8) {
			bytes[i] = 1;
			bytes[i + 1] = 1;
			bytes[i + 2] = 1;
			bytes[i + 3] = 1;
			bytes[i + 4] = 1;
			bytes[i + 5] = 1;
			bytes[i + 6] = 1;
			bytes[i + 7] = 1;
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToHeap() {
		for (int i = 0; i < 1024; i += 8) {
			heapByteBuffer.putLong(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToDirect() {
		for (int i = 0; i < 1024; i += 8) {
			directByteBuffer.putLong(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToNetty() {
		for (int i = 0; i < 1024; i += 8) {
			nettyByteBuf.setLong(i, 1);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToDirectAndCopyToNetty() {
		for (int i = 0; i < 1024; i += 8) {
			directByteBuffer.putLong(i, 1);
		}

		nettyByteBuf.setBytes(0, directByteBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeLongToUnsafe() {
		for (int i = 0; i < 1024; i += 8) {
			UnsafeUtils.unsafe().putInt(OFFSET + i, 1);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(ByteBufferBenchmark.class.getName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(8)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
