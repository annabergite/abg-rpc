package rpc.abg.benchmark.httpparam;

import java.net.URLDecoder;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

@State(Scope.Benchmark)
public class HttpParamExtractBenchmark {

	private final String uri = "/group/app/get-user?v=1&userId=123456&name=%e7%8e%8b%e5%b0%bc%e7%8e%9b";
	private final String json = "{\"userId\": 123456,\"name\": \"王尼玛\"}";

	private static final ObjectMapper mapper = new ObjectMapper()//
			.registerModule(new Jdk8Module())//
			.registerModule(new JavaTimeModule())//
			.registerModule(new AfterburnerModule());

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ParamBean urlExtractToBean() throws Exception {
		int paramBeginIndex = 0;

		long userId = 0;
		String name = null;

		{
			String paramKey = "&v=";
			int paramKeyLength = paramKey.length();

			int indexBegin = uri.indexOf(paramKey) + paramKeyLength;
			int indexEnd = uri.indexOf('&', indexBegin);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			paramBeginIndex = indexEnd;
		}

		{
			String paramKey = "&userId=";
			int paramKeyLength = paramKey.length();

			int indexBegin = uri.indexOf(paramKey, paramBeginIndex) + paramKeyLength;
			int indexEnd = uri.indexOf('&', indexBegin);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			String param = uri.substring(indexBegin, indexEnd);
			userId = Integer.parseInt(param);
		}

		{
			String paramKey = "&name=";
			int paramKeyLength = paramKey.length();

			int indexBegin = uri.indexOf(paramKey, paramBeginIndex) + paramKeyLength;
			int indexEnd = uri.indexOf('&', indexBegin);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			name = uri.substring(indexBegin, indexEnd);

			if (name.indexOf('%') > -1) {
				name = URLDecoder.decode(name, "UTF-8");
			}
		}

		ParamBean bean = new ParamBean();
		bean.setUserId(userId);
		bean.setName(name);

		return bean;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Map<String, Object> urlExtractToMap() throws Exception {
		int offset = 0;

		{
			String paramKey = "&v=";
			int paramKeyLength = paramKey.length();

			int indexBegin = uri.indexOf(paramKey) + paramKeyLength;
			int indexEnd = uri.indexOf('&', indexBegin);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			offset = indexEnd;
		}

		Map<String, Object> map = new HashMap<>();

		while (true) {
			int indexBegin = uri.indexOf('&', offset);

			if (indexBegin < 0) {
				break;
			}

			indexBegin += 1;
			offset = indexBegin;

			int indexEnd = uri.indexOf('=', offset);
			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;

			String key = uri.substring(indexBegin, indexEnd);

			indexBegin = indexEnd + 1;
			offset = indexBegin;
			indexEnd = uri.indexOf('&', offset);

			indexEnd = indexEnd < 0 ? uri.length() : indexEnd;
			offset = indexEnd;

			String value = uri.substring(indexBegin, indexEnd);

			if (value.indexOf('%') > -1) {
				value = URLDecoder.decode(value, "UTF-8");
			}

			map.put(key, value);
		}

		return map;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public ParamBean jsonExtractToBean() throws Exception {
		ParamBean bean = mapper.readValue(json, ParamBean.class);
		return bean;
	}

	public static void main(String[] args) throws Exception {

		Options opt = new OptionsBuilder()//
				.include(HttpParamExtractBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(4)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
