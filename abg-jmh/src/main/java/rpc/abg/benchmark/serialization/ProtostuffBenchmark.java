package rpc.abg.benchmark.serialization;

import java.util.Map;
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
import io.protostuff.ByteBufInput;
import io.protostuff.ByteBufOutput;
import io.protostuff.Schema;
import io.protostuff.runtime.FastIdStrategy;
import io.protostuff.runtime.RuntimeSchema;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;

@State(Scope.Thread)
public class ProtostuffBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
	ByteBuf userBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);
	ByteBuf listBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);
	ByteBuf wrapUserBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);

	FastIdStrategy fastIdStrategy = new FastIdStrategy();

	private final Schema<User> userSchema = RuntimeSchema.getSchema(User.class);
	@SuppressWarnings("rawtypes")
	private final Schema<Page> userPageSchema;

	private final Schema<WrapUser> wrapUserSchema;

	private final UserService userService = new UserServiceServerImpl();

	private final WrapUser wrapUser = new WrapUser();
	private final User user = userService.getUser(123456789L).join();
	private final Page<User> userPage = userService.listUser(0).join();

	public ProtostuffBenchmark() {

		fastIdStrategy.registerPojoID(Map.of(User.class.getName(), 0, Page.class.getName(), 1));

		userPageSchema = RuntimeSchema.getSchema(Page.class, fastIdStrategy);
		wrapUserSchema = RuntimeSchema.getSchema(WrapUser.class, fastIdStrategy);

		try {
			userBuffer.clear();
			ByteBufOutput output = new ByteBufOutput(userBuffer);
			userSchema.writeTo(output, user);

			System.out.println("userBytes.length：" + userBuffer.writerIndex());
			System.out.println(deserializeUser());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			listBuffer.clear();
			ByteBufOutput output = new ByteBufOutput(listBuffer);
			userPageSchema.writeTo(output, userPage);

			System.out.println("userPageBytes.length：" + listBuffer.writerIndex());
			System.out.println(new String(ByteBufUtil.getBytes(listBuffer)));
			System.out.println(deserializeUserPage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			wrapUser.setUser(user);

			wrapUserBuffer.clear();
			ByteBufOutput output = new ByteBufOutput(wrapUserBuffer);
			wrapUserSchema.writeTo(output, wrapUser);

			System.out.println("wrapUserBytes.length：" + wrapUserBuffer.writerIndex());

			System.out.println(new String(ByteBufUtil.getBytes(wrapUserBuffer)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUser() throws Exception {
		userBuffer.clear();
		ByteBufOutput output = new ByteBufOutput(userBuffer);
		userSchema.writeTo(output, user);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public User deserializeUser() throws Exception {
		userBuffer.readerIndex(0);
		ByteBufInput input = new ByteBufInput(userBuffer, true);

		User user = userSchema.newMessage();
		userSchema.mergeFrom(input, user);

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUserList() throws Exception {
		listBuffer.clear();
		ByteBufOutput output = new ByteBufOutput(listBuffer);
		userPageSchema.writeTo(output, userPage);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@SuppressWarnings("unchecked")
	public Page<User> deserializeUserPage() throws Exception {
		listBuffer.readerIndex(0);
		ByteBufInput input = new ByteBufInput(listBuffer, true);

		Page<User> userPage = (Page<User>) userPageSchema.newMessage();
		userPageSchema.mergeFrom(input, userPage);

		return userPage;
	}

	public static void main(String[] args) throws Exception {

		ProtostuffBenchmark benchmark = new ProtostuffBenchmark();
		benchmark.serializeUser();

		Options opt = new OptionsBuilder()//
				.include("rpc.abg.benchmark.serialization.ProtostuffBenchmark")//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
