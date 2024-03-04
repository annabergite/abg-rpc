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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.protostuff.ByteBufInput;
import io.protostuff.ByteBufOutput;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import rpc.abg.serialization.TracerSerializer;
import rpc.abg.serialization.kryo.FastClassResolver;
import rpc.abg.trace.Tracer;
import rpc.abg.util.uuid.ObjectId;

@State(Scope.Thread)
public class TracerSerializerBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	ByteBufAllocator allocator = new UnpooledByteBufAllocator(true);

	ByteBuf protostuffBuffer = allocator.directBuffer(1024 * 1024, 1024 * 1024);
	ByteBuf kryoBuffer = allocator.directBuffer(1024 * 1024, 1024 * 1024);
	ByteBuf tracerSerializerBuffer = allocator.directBuffer(1024 * 1024, 1024 * 1024);

	private final Schema<Tracer> tracerSchema = RuntimeSchema.getSchema(Tracer.class);
	private final Kryo kryo = new Kryo(new FastClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
	private final TracerSerializer tracerSerializer = new TracerSerializer();

	private final Tracer tracer = new Tracer();

	public TracerSerializerBenchmark() {
		tracer.setTraceId(ObjectId.next());
		tracer.setSpanId(123);
		tracer.setParentId(456);

		try {
			writeByProtostuff();

			System.out.println("protostuffBuffer.length：" + protostuffBuffer.writerIndex());

			System.out.println(readByProtostuff());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			writeByKryo();

			System.out.println("kryoBuffer.length：" + kryoBuffer.writerIndex());

			System.out.println(readByKryo());
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			writeByTracerSerializer();

			System.out.println("tracerSerializerBuffer.length：" + tracerSerializerBuffer.writerIndex());

			System.out.println(readByTracerSerializer());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByProtostuff() throws Exception {
		protostuffBuffer.clear();
		ByteBufOutput output = new ByteBufOutput(protostuffBuffer);
		tracerSchema.writeTo(output, tracer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Tracer readByProtostuff() throws Exception {
		protostuffBuffer.readerIndex(0);
		ByteBufInput input = new ByteBufInput(protostuffBuffer, true);

		Tracer tracer = tracerSchema.newMessage();
		tracerSchema.mergeFrom(input, tracer);

		return tracer;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByKryo() throws Exception {
		kryoBuffer.clear();
		rpc.abg.serialization.kryo.ByteBufOutput output = new rpc.abg.serialization.kryo.ByteBufOutput(kryoBuffer);
		kryo.writeObject(output, tracer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Tracer readByKryo() throws Exception {
		kryoBuffer.readerIndex(0);

		rpc.abg.serialization.kryo.ByteBufInput input = new rpc.abg.serialization.kryo.ByteBufInput(kryoBuffer);
		Tracer tracer = kryo.readObject(input, Tracer.class);

		return tracer;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void writeByTracerSerializer() throws Exception {
		tracerSerializerBuffer.clear();
		tracerSerializer.write(tracerSerializerBuffer, tracer);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Tracer readByTracerSerializer() throws Exception {
		tracerSerializerBuffer.readerIndex(0);
		Tracer tracer = tracerSerializer.read(tracerSerializerBuffer);
		return tracer;
	}

	public static void main(String[] args) throws Exception {

		TracerSerializerBenchmark benchmark = new TracerSerializerBenchmark();
		benchmark.readByKryo();

		Options opt = new OptionsBuilder()//
				.include(TracerSerializerBenchmark.class.getName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();

	}

}
