package rpc.abg.benchmark.map;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;

/**
 * 
 * @author Annabergite
 *
 */
@State(Scope.Benchmark)
public class MapVsThreadLocalBenchmark {
	public static final int CONCURRENCY = Runtime.getRuntime().availableProcessors();

	private final UserService userService = new UserServiceServerImpl();

	private final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();
	private final HashMap<Integer, User> userMap = new HashMap<>();
	private final ConcurrentHashMap<Integer, User> userConcurrentMap = new ConcurrentHashMap<>();
	private final CopyOnWriteArrayList<User> userList = new CopyOnWriteArrayList<>();
	private User directUser = userService.getUser(12345L).join();
	private volatile User volatileUser = userService.getUser(12345L).join();

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User directGet() {
		User user = directUser;

		if (user != null && user.getId() == 12345L) {
			return user;
		}

		user = userService.getUser(12345L).join();
		directUser = user;

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User volatileGet() {
		User user = volatileUser;

		if (user != null && user.getId() == 12345L) {
			return user;
		}

		user = userService.getUser(12345L).join();
		volatileUser = user;

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User copyOnWriteArrayList() {
		if (userList.isEmpty()) {
			synchronized (userList) {
				if (userList.isEmpty()) {
					for (int i = 0; i < 12345 + 1; i++) {
						User user = userService.getUser(i).join();
						userMap.put(i, user);
						userConcurrentMap.put(i, user);
						userList.add(user);
					}
				}
			}

		}

		User user = userList.get(12345);

		if (user != null) {
			return user;
		}

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User threadLocal() {
		User user = userThreadLocal.get();

		if (user != null && user.getId() == 12345L) {
			return user;
		}

		user = userService.getUser(12345L).join();
		userThreadLocal.set(user);

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User concurrentHashMap() {
		User user = userConcurrentMap.get(12345);

		if (user != null) {
			return user;
		}

		user = userService.getUser(12345L).join();
		userConcurrentMap.put(12345, user);

		return user;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public User hashMap() {
		User user = userMap.get(12345);

		if (user != null) {
			return user;
		}

		return user;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()//
				.include(MapVsThreadLocalBenchmark.class.getSimpleName())//
				.warmupIterations(5)//
				.measurementIterations(5)//
				.threads(CONCURRENCY)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}

}
