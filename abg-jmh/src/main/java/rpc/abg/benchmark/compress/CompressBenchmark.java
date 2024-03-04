package rpc.abg.benchmark.compress;

import java.io.IOException;
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
import org.xerial.snappy.Snappy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.protostuff.ByteBufOutput;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;

@State(Scope.Thread)
public class CompressBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
	ByteBuf listBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);

	ByteBuffer lz4RawBuffer;
	ByteBuffer lz4CompressedBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 8);

	ByteBuffer snappyRawBuffer;
	ByteBuffer snappyCompressedBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 8);

	@SuppressWarnings("rawtypes")
	private final Schema<Page> userPageSchema = RuntimeSchema.getSchema(Page.class);
	private final UserService userService = new UserServiceServerImpl();
	private final Page<User> userPage = userService.listUser(0).join();

	LZ4Compressor lz4Compressor = LZ4Factory.nativeInstance().fastCompressor();
	LZ4FastDecompressor decompressor = LZ4Factory.nativeInstance().fastDecompressor();

	public CompressBenchmark() {
		try {
			listBuffer.clear();
			ByteBufOutput output = new ByteBufOutput(listBuffer);
			userPageSchema.writeTo(output, userPage);

			lz4RawBuffer = listBuffer.nioBuffer();
			snappyRawBuffer = listBuffer.nioBuffer();

			System.out.println("lz4RawBuffer：" + lz4RawBuffer);
			System.out.println("snappyRawBuffer：" + snappyRawBuffer);

			lz4Compress();
			lz4CompressedBuffer.flip();

			snappyCompress();
			// snappyCompressedBuffer.flip();

			System.out.println("lz4CompressedBuffer：" + lz4CompressedBuffer);
			System.out.println("snappyCompressedBuffer：" + snappyCompressedBuffer);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void lz4Compress() {
		lz4RawBuffer.position(0);
		lz4CompressedBuffer.clear();

		lz4Compressor.compress(lz4RawBuffer, lz4CompressedBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void lz4Decompress() {
		lz4CompressedBuffer.position(0);
		lz4RawBuffer.clear();

		decompressor.decompress(lz4CompressedBuffer, lz4RawBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void snappyCompress() {
		snappyRawBuffer.position(0);
		snappyCompressedBuffer.clear();

		try {
			Snappy.compress(snappyRawBuffer, snappyCompressedBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void snappyDecompress() {
		snappyCompressedBuffer.position(0);
		snappyRawBuffer.clear();

		try {
			Snappy.uncompress(snappyCompressedBuffer, snappyRawBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(CompressBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
