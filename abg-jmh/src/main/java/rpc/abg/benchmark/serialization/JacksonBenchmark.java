package rpc.abg.benchmark.serialization;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.serialization.jackson.JacksonMapper;

@State(Scope.Thread)
public class JacksonBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private static final ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();

		mapper.registerModule(new Jdk8Module());

		JavaTimeModule javaTimeModule = new JavaTimeModule();
		// Hack time module to allow 'Z' at the end of string (i.e. javascript json's)
		javaTimeModule.addDeserializer(LocalDateTime.class,
				new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
		mapper.registerModule(javaTimeModule);

		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		mapper.registerModule(new AfterburnerModule());
	}

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
	ByteBuf userBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);
	ByteBuf listBuffer = allocator.directBuffer(1024 * 1024 * 8, 1024 * 1024 * 8);

	ByteBuffer nioBuffer = ByteBuffer.allocateDirect(1024 * 1024 * 8);
	ByteBufferOutputStream nioOutputStream = new ByteBufferOutputStream(nioBuffer);
	ByteBufferInputStream nioInputStream = new ByteBufferInputStream(nioBuffer);

	private final JacksonMapper jacksonMapper = new JacksonMapper();

	private final UserService userService = new UserServiceServerImpl();

	private final User user = userService.getUser(123456789L).join();
	private final Page<User> userPage = userService.listUser(0).join();

	public JacksonBenchmark() {

		try {
			serializeUser();

			System.out.println("userBytes.length：" + userBuffer.writerIndex());

			System.out.println(deserializeUser());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			serializeUserList();

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
		jacksonMapper.write(userBuffer, user);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public User deserializeUser() throws Exception {
		userBuffer.readerIndex(0);
		return jacksonMapper.read(userBuffer, User.class);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUserList() throws Exception {
		listBuffer.clear();
		jacksonMapper.write(listBuffer, userPage);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUser2() throws Exception {
		nioBuffer.clear();
		userBuffer.clear();

		mapper.writeValue(nioOutputStream, user);

		nioBuffer.flip();
		userBuffer.writeBytes(nioBuffer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void serializeUserList2() throws Exception {
		nioBuffer.clear();
		listBuffer.clear();

		mapper.writeValue(nioOutputStream, userPage);

		nioBuffer.flip();
		listBuffer.writeBytes(nioBuffer);
	}

	public static void main(String[] args) throws Exception {

		Options opt = new OptionsBuilder()//
				.include(JacksonBenchmark.class.getName())//
				.warmupIterations(10)//
				.measurementIterations(10)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
