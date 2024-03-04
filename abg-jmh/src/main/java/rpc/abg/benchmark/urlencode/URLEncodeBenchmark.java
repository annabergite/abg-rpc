package rpc.abg.benchmark.urlencode;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.net.URLCodec;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import rpc.abg.util.URLEncodeUtils;

@State(Scope.Benchmark)
public class URLEncodeBenchmark {

	Escaper urlEscaper = UrlEscapers.urlPathSegmentEscaper();
	private static final URLCodec codecer = new URLCodec("UTF-8");

	private final String str = "aZ Hello, World, 你好，世界！   +++ ---- %%%% @@@";
	private final String encoded = URLEncodeUtils.encode(str);

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String encodeByURLEncodeUtils() throws Exception {
		return URLEncodeUtils.encode(str);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String encodeByJavaURLEncoder() throws Exception {
		return URLEncoder.encode(str, "UTF-8");
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String encodeByCommonCodecURLCodec() throws Exception {
		return codecer.encode(str);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String encodeByGuavaUrlEscapers() throws Exception {
		return urlEscaper.escape(str);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String decodeByURLEncodeUtils() throws Exception {
		return URLEncodeUtils.decode(encoded, StandardCharsets.UTF_8);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String decodeByJavaURLEncoder() throws Exception {
		return URLDecoder.decode(encoded, "UTF-8");
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public String decodeByCommonCodecURLCodec() throws Exception {
		return codecer.decode(encoded);
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()//
				.include(URLEncodeBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(4)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
