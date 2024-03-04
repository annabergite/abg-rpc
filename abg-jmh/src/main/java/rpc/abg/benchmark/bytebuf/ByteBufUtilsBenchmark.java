package rpc.abg.benchmark.bytebuf;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import rpc.abg.util.ByteBufUtils;

@State(Scope.Thread)
public class ByteBufUtilsBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new PooledByteBufAllocator(true);
	ByteBuf buffer = allocator.directBuffer(1024, 1024);

	@TearDown
	public void close() {
		buffer.release();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufMove() {
		buffer.writerIndex(0);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetByte() {
		buffer.setByte(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetShort() {
		buffer.setShort(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetMedium() {
		buffer.setMedium(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetInt() {
		buffer.setInt(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetIntLE() {
		buffer.setIntLE(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void byteBufSetLong() {
		buffer.setLong(0, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeVarInt_1() {
		buffer.writerIndex(0);
		ByteBufUtils.writeVarInt(buffer, 1);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeVarInt_1024() {
		buffer.writerIndex(0);
		ByteBufUtils.writeVarInt(buffer, 1024);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeVarInt_65539() {
		buffer.writerIndex(0);
		ByteBufUtils.writeVarInt(buffer, 65539);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeVarInt_16777216() {
		buffer.writerIndex(0);
		ByteBufUtils.writeVarInt(buffer, 16777216);
	}

	public static void main(String[] args) throws RunnerException {
		// ByteBufAllocator allocator = new PooledByteBufAllocator(true);
		// ByteBuf buffer = allocator.directBuffer(1024, 1024);
		//
		// buffer.clear();
		// ByteBufUtils.writeVarInt(buffer, -1024);
		//
		// System.out.println(ByteBufUtil.hexDump(buffer));
		//
		// buffer.clear();
		// ByteBufUtils.writeVarIntDirect(buffer, -1024);
		//
		// System.out.println(ByteBufUtil.hexDump(buffer));
		//
		// System.out.println("ByteBufUtils.writeVarInt:");
		// for (int i = 0; i < 5; i++) {
		// int value = 1 << (i * 8);
		//
		// buffer.clear();
		//
		// ByteBufUtils.writeVarInt(buffer, value);
		//
		// buffer.readerIndex(0);
		// int read = ByteBufUtils.readVarInt(buffer);
		//
		// System.out.println(value + ":" + read);
		// }
		//
		// System.out.println("ByteBufUtils.writeVarLong:");
		// for (int i = 0; i < 9; i++) {
		// long value = 1L << (i * 8);
		//
		// buffer.clear();
		//
		// ByteBufUtils.writeVarLong(buffer, value);
		//
		// buffer.readerIndex(0);
		// long read = ByteBufUtils.readVarLong(buffer);
		//
		// System.out.println(value + ":" + read);
		// }

		Options opt = new OptionsBuilder()//
				.include(ByteBufUtilsBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
