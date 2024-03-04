package rpc.abg.benchmark.serialization;

import java.util.ArrayList;
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.serialization.kryo.ByteBufInput;
import rpc.abg.serialization.kryo.ByteBufOutput;
import rpc.abg.serialization.kryo.FastClassResolver;
import rpc.abg.serialization.kryo.FastSerializer;

@State(Scope.Thread)
public class KryoBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
	ByteBuf userBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);
	ByteBuf listBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);

	private final Kryo kryo = new Kryo(new FastClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());

	private final ByteBufOutput output = new ByteBufOutput(null);
	private final ByteBufInput input = new ByteBufInput(null);

	private final UserService userService = new UserServiceServerImpl();

	private final User user = userService.getUser(123456789L).join();
	private final Page<User> userPage = userService.listUser(0).join();

	public KryoBenchmark() {
		kryo.setDefaultSerializer(FastSerializer.class);
		kryo.setReferences(false);

		kryo.register(User.class);
		kryo.register(Page.class);
		kryo.register(ArrayList.class);

		try {
			userBuffer.clear();
			output.setBuffer(userBuffer);
			kryo.writeClassAndObject(output, user);

			System.out.println(new String(ByteBufUtil.getBytes(userBuffer.duplicate())));
			System.out.println(deserializeUser());
			System.out.println("userBytes.length：" + userBuffer.writerIndex());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			listBuffer.clear();
			output.setBuffer(listBuffer);
			kryo.writeClassAndObject(output, userPage);

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
		output.setBuffer(userBuffer);
		kryo.writeClassAndObject(output, user);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public User deserializeUser() throws Exception {
		userBuffer.readerIndex(0);
		input.setBuffer(userBuffer);

		return (User) kryo.readClassAndObject(input);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUserList() throws Exception {
		listBuffer.clear();
		output.setBuffer(listBuffer);
		kryo.writeClassAndObject(output, userPage);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	@SuppressWarnings("unchecked")
	public Page<User> deserializeUserPage() throws Exception {
		listBuffer.readerIndex(0);
		input.setBuffer(listBuffer);

		return (Page<User>) kryo.readClassAndObject(input);
	}

	public static void main(String[] args) throws Exception {

		Options opt = new OptionsBuilder()//
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
