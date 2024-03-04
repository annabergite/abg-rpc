package rpc.abg.benchmark.kryo;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.FastInput;
import com.esotericsoftware.kryo.io.FastOutput;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.serialization.kryo.ByteBufInput;
import rpc.abg.serialization.kryo.ByteBufOutput;

@State(Scope.Thread)
public class DefaultBenchmark {

	private final UserService userService = new UserServiceServerImpl();
	private final User user = userService.getUser(123456789L).join();
	private final Page<User> page = userService.listUser(0).join();

	final byte[] bytes = new byte[1024 * 1024];
	final ByteBuffer heapByteBuffer = ByteBuffer.allocate(1024 * 1024);
	final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(1024 * 1024);
	final ByteBuf nettyByteBuf = PooledByteBufAllocator.DEFAULT.buffer(1024 * 1024, 1024 * 1024);

	final Kryo kryo = new Kryo();

	final FastInput fastInput = new FastInput(bytes);
	final FastOutput fastOutput = new FastOutput(bytes);

	final UnsafeInput unsafeInput = new UnsafeInput(bytes);
	final UnsafeOutput unsafeOutput = new UnsafeOutput(bytes);

	final ByteBufferInput heapByteBufferInput = new ByteBufferInput(heapByteBuffer);
	final ByteBufferOutput heapByteBufferOutput = new ByteBufferOutput(heapByteBuffer);

	final ByteBufferInput directByteBufferInput = new ByteBufferInput(directByteBuffer);
	final ByteBufferOutput directByteBufferOutput = new ByteBufferOutput(directByteBuffer);

	final ByteBufInput nettyByteBufInput = new ByteBufInput(nettyByteBuf);
	final ByteBufOutput nettyByteBufOutput = new ByteBufOutput(nettyByteBuf);

	@Setup
	public void init() {
		kryo.setReferences(false);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void fastUser() {
		fastOutput.clear();
		kryo.writeObject(fastOutput, user);

		fastInput.setBuffer(bytes, 0, (int) fastOutput.total());
		kryo.readObject(fastInput, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void fastPage() {
		fastOutput.clear();
		kryo.writeObject(fastOutput, page);

		fastInput.setBuffer(bytes, 0, (int) fastOutput.total());
		kryo.readObject(fastInput, Page.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void unsafeUser() {
		unsafeOutput.clear();
		kryo.writeObject(unsafeOutput, user);

		unsafeInput.setBuffer(bytes, 0, (int) unsafeOutput.total());
		kryo.readObject(unsafeInput, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void unsafePage() {
		unsafeOutput.clear();
		kryo.writeObject(unsafeOutput, page);

		unsafeInput.setBuffer(bytes, 0, (int) unsafeOutput.total());
		kryo.readObject(unsafeInput, Page.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void heapByteBufferUser() {
		heapByteBuffer.clear();
		heapByteBufferOutput.setBuffer(heapByteBuffer);
		kryo.writeObject(heapByteBufferOutput, user);

		heapByteBuffer.flip();
		heapByteBufferInput.setBuffer(heapByteBuffer);
		kryo.readObject(heapByteBufferInput, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void heapByteBufferPage() {
		heapByteBuffer.clear();
		heapByteBufferOutput.setBuffer(heapByteBuffer);
		kryo.writeObject(heapByteBufferOutput, page);

		heapByteBuffer.flip();
		heapByteBufferInput.setBuffer(heapByteBuffer);
		kryo.readObject(heapByteBufferInput, Page.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void directByteBufferUser() {
		directByteBuffer.clear();
		directByteBufferOutput.setBuffer(directByteBuffer);
		kryo.writeObject(directByteBufferOutput, user);

		directByteBuffer.flip();
		directByteBufferInput.setBuffer(directByteBuffer);
		kryo.readObject(directByteBufferInput, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void directByteBufferPage() {
		directByteBuffer.clear();
		directByteBufferOutput.setBuffer(directByteBuffer);
		kryo.writeObject(directByteBufferOutput, page);

		directByteBuffer.flip();
		directByteBufferInput.setBuffer(directByteBuffer);
		kryo.readObject(directByteBufferInput, Page.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void nettyByteBufUser() {
		nettyByteBuf.clear();
		nettyByteBufOutput.setBuffer(nettyByteBuf);
		kryo.writeObject(nettyByteBufOutput, user);

		nettyByteBufInput.setBuffer(nettyByteBuf);
		kryo.readObject(nettyByteBufInput, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void nettyByteBufPage() {
		nettyByteBuf.clear();
		nettyByteBufOutput.setBuffer(nettyByteBuf);
		kryo.writeObject(nettyByteBufOutput, page);

		nettyByteBufInput.setBuffer(nettyByteBuf);
		kryo.readObject(nettyByteBufInput, Page.class);
	}

	public static void main(String[] args) throws RunnerException {
		DefaultBenchmark benchmark = new DefaultBenchmark();
		benchmark.directByteBufferPage();

		Options opt = new OptionsBuilder()//
				.include(DefaultBenchmark.class.getName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(8)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
