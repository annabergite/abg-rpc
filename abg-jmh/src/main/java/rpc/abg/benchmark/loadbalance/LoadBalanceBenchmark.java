package rpc.abg.benchmark.loadbalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

import rpc.abg.loadbalance.LoadBalance;
import rpc.abg.loadbalance.RandomLoadBalance;
import rpc.abg.loadbalance.RoundRobinLoadBalance;
import rpc.abg.loadbalance.Weightable;

@State(Scope.Benchmark)
public class LoadBalanceBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final LoadBalance<Weightable> randomLoadBalance1 = new RandomLoadBalance<>();
	private final LoadBalance<Weightable> roundRobinLoadBalance1 = new RoundRobinLoadBalance<>();

	private final LoadBalance<Weightable> randomLoadBalance10 = new RandomLoadBalance<>();
	private final LoadBalance<Weightable> roundRobinLoadBalance10 = new RoundRobinLoadBalance<>();

	private final LoadBalance<Weightable> randomLoadBalance100 = new RandomLoadBalance<>();
	private final LoadBalance<Weightable> roundRobinLoadBalance100 = new RoundRobinLoadBalance<>();

	public LoadBalanceBenchmark() {
		List<Weightable> weightables = new ArrayList<>();
		for (int i = 0; i < 1; i++) {
			weightables.add(new WeightBean(i, 100));
		}

		randomLoadBalance1.setWeightables(weightables);
		roundRobinLoadBalance1.setWeightables(weightables);

		weightables = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			weightables.add(new WeightBean(i, 100));
		}

		randomLoadBalance10.setWeightables(weightables);
		roundRobinLoadBalance10.setWeightables(weightables);

		weightables = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			weightables.add(new WeightBean(i, 100));
		}

		randomLoadBalance100.setWeightables(weightables);
		roundRobinLoadBalance100.setWeightables(weightables);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable randomLoadBalance1() {
		return randomLoadBalance1.select();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable roundRobinLoadBalance1() {
		return roundRobinLoadBalance1.select();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable randomLoadBalance10() {
		return randomLoadBalance10.select();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable roundRobinLoadBalance10() {
		return roundRobinLoadBalance10.select();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable randomLoadBalance100() {
		return randomLoadBalance100.select();
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public Weightable roundRobinLoadBalance100() {
		return roundRobinLoadBalance100.select();
	}

	public static void main(String[] args) throws RunnerException {
		LoadBalance<Weightable> randomLoadBalance100 = new RandomLoadBalance<>();
		LoadBalance<Weightable> roundRobinLoadBalance100 = new RoundRobinLoadBalance<>();

		List<Weightable> weightables = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			weightables.add(new WeightBean(i, i));
		}

		randomLoadBalance100.setWeightables(weightables);
		roundRobinLoadBalance100.setWeightables(weightables);

		Map<Integer, Integer> map = new TreeMap<>();
		for (int i = 0; i < 1000000; i++) {
			WeightBean weightBean = (WeightBean) randomLoadBalance100.select();

			int count = map.getOrDefault(weightBean.index, 0) + 1;
			map.put(weightBean.index, count);
		}

		map.replaceAll((k, v) -> (int) Math.round(v / 5000D));

		System.out.println("randomLoadBalance100: " + map);

		map = new TreeMap<>();
		for (int i = 0; i < 1000000; i++) {
			WeightBean weightBean = (WeightBean) roundRobinLoadBalance100.select();

			int count = map.getOrDefault(weightBean.index, 0) + 1;
			map.put(weightBean.index, count);
		}

		map.replaceAll((k, v) -> (int) Math.round(v / 5000D));
		System.out.println("roundRobinLoadBalance100: " + map);

		Options opt = new OptionsBuilder()//
				.include(LoadBalanceBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
