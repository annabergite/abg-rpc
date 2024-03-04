package rpc.abg.benchmark.string;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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

import rpc.abg.util.UnsafeStringUtils;
import rpc.abg.util.UnsafeUtils;

@State(Scope.Benchmark)
public class StringTest {
	private static final Method isLatin1Method;

	static {
		Method _isLatin1Method = null;

		try {
			_isLatin1Method = String.class.getDeclaredMethod("isLatin1");

			if (!_isLatin1Method.trySetAccessible()) {
				_isLatin1Method = null;
			}

			if (!(boolean) _isLatin1Method.invoke("hello")) {
				_isLatin1Method = null;
			}
		} catch (Exception e) {
			_isLatin1Method = null;
		}

		isLatin1Method = _isLatin1Method;
	}

	private final String str = "https://github.com/twitter/finagle/blob/master/finagle-netty4/src/main/scala/com/twitter/finagle/netty4/Netty4Listener.scala";

	ThreadLocal<byte[]> bufferHolder = ThreadLocal.withInitial(() -> new byte[str.length()]);

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte[] getBytes() throws Exception {
		return str.getBytes(StandardCharsets.UTF_8);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	@SuppressWarnings("deprecation")
	public byte[] getBytesAscii() throws Exception {
		boolean ascii = false;
		if (isLatin1Method != null) {
			try {
				ascii = (boolean) isLatin1Method.invoke(str);
			} catch (Exception e) {
				ascii = false;
			}
		}

		if (ascii) {
			int length = str.length();
			byte[] buffer = new byte[length];
			str.getBytes(0, length, buffer, 0);
			return buffer;
		} else {
			return str.getBytes(StandardCharsets.UTF_8);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	@SuppressWarnings("deprecation")
	public byte[] getBytesAsciiWithBuffer() throws Exception {
		boolean ascii = false;
		if (isLatin1Method != null) {
			try {
				ascii = (boolean) isLatin1Method.invoke(str);
			} catch (Exception e) {
				ascii = false;
			}
		}

		if (ascii) {
			byte[] buffer = bufferHolder.get();
			str.getBytes(0, str.length(), buffer, 0);
			return buffer;
		} else {
			return str.getBytes(StandardCharsets.UTF_8);
		}
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte[] StringUtils2_getUTF8Bytes() throws Exception {
		return UnsafeStringUtils.getUTF8Bytes(str);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public byte[] StringUtils2_getLatin1Bytes() throws Exception {
		return UnsafeStringUtils.getLatin1Bytes(str);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String instanceByNew() {
		return new String();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String instanceByUnsafe() throws Exception {
		return (String) UnsafeUtils.unsafe().allocateInstance(String.class);
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()//
				.include(StringTest.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(4)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
