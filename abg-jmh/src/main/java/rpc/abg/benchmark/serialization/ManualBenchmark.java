package rpc.abg.benchmark.serialization;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.serialization.manual.UserPageSerializer;
import rpc.abg.benchmark.serialization.manual.UserSerializer;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;

@State(Scope.Thread)
public class ManualBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
	ByteBuf userBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);
	ByteBuf listBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);

	private final static UserSerializer userSerializer = new UserSerializer();
	private final static UserPageSerializer userPageSerializer = new UserPageSerializer();

	private final UserService userService = new UserServiceServerImpl();

	private final User user = userService.getUser(123456789L).join();
	private final Page<User> userPage = userService.listUser(0).join();

	public ManualBenchmark() {

		try {
			serializeUser();
			System.out.println(new String(ByteBufUtil.getBytes(userBuffer.duplicate())));

			System.out.println(deserializeUser());
			System.out.println("userBytes.length：" + userBuffer.writerIndex());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			serializeUserList();
			System.out.println(new String(ByteBufUtil.getBytes(listBuffer.duplicate())));

			System.out.println(deserializeUserPage());
			System.out.println("userPageBytes.length：" + listBuffer.writerIndex());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUser() throws Exception {
		userBuffer.clear();
		userSerializer.write(userBuffer, user);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public User deserializeUser() throws Exception {
		userBuffer.readerIndex(0);
		return userSerializer.read(userBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUserList() throws Exception {
		listBuffer.clear();
		userPageSerializer.write(listBuffer, userPage);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public Page<User> deserializeUserPage() throws Exception {
		listBuffer.readerIndex(0);
		return userPageSerializer.read(listBuffer);
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()//
				.include(ManualBenchmark.class.getName())//
				.include(KryoBenchmark.class.getName())//
				.include(ProtostuffBenchmark.class.getName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
