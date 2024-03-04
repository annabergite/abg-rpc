package rpc.abg.benchmark.reflect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import com.esotericsoftware.reflectasm.MethodAccess;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import rpc.abg.util.UnsafeUtils;

@State(Scope.Benchmark)
public class ReflectBenchmark {

	private static final Field directUserAgeField;
	private static final Method getDirectUserAgeMethod;
	private static final MethodHandle getDirectUserAgeMethodHandle;
	private static final VarHandle directUserAgeVarHandle;
	private static final long directUserAgeFieldOffset;
	private static final FastClass cglibFastClass = FastClass.create(ReflectTestBean.class);
	private static final FastMethod cgligFastMethod;
	private static final MethodAccess reflectasmMethodAccess = MethodAccess.get(ReflectTestBean.class);
	private static final int reflectasmMethodIndex = reflectasmMethodAccess.getIndex("getDirectUserAge", long.class);

	static {
		{
			Method _getDirectUserAgeMethod = null;

			try {
				_getDirectUserAgeMethod = ReflectTestBean.class.getDeclaredMethod("getDirectUserAge", long.class);

				if (!_getDirectUserAgeMethod.trySetAccessible()) {
					_getDirectUserAgeMethod = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				_getDirectUserAgeMethod = null;
			}

			getDirectUserAgeMethod = _getDirectUserAgeMethod;

			cgligFastMethod = cglibFastClass.getMethod(getDirectUserAgeMethod);
		}

		{
			Field _directUserAgeField = null;
			try {
				_directUserAgeField = ReflectTestBean.class.getDeclaredField("directUserAge");

				if (!_directUserAgeField.trySetAccessible()) {
					_directUserAgeField = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
				_directUserAgeField = null;
			}

			directUserAgeField = _directUserAgeField;
		}

		{
			MethodHandle _getDirectUserAgeMethod = null;

			try {
				_getDirectUserAgeMethod = MethodHandles.privateLookupIn(ReflectTestBean.class, MethodHandles.lookup())
						.findVirtual(ReflectTestBean.class, "getDirectUserAge",
								MethodType.methodType(int.class, long.class));
			} catch (Exception e) {
				e.printStackTrace();
				_getDirectUserAgeMethod = null;
			}

			getDirectUserAgeMethodHandle = _getDirectUserAgeMethod;
		}

		{
			VarHandle _directUserAgeVarHandle = null;
			try {
				_directUserAgeVarHandle = MethodHandles.privateLookupIn(ReflectTestBean.class, MethodHandles.lookup())
						.findVarHandle(ReflectTestBean.class, "directUserAge", int.class);
			} catch (Exception e) {
				e.printStackTrace();
				_directUserAgeVarHandle = null;
			}

			directUserAgeVarHandle = _directUserAgeVarHandle;
		}

		{
			long _directUserAgeFieldOffset = 0;
			try {
				Field coderField = ReflectTestBean.class.getDeclaredField("directUserAge");
				_directUserAgeFieldOffset = UnsafeUtils.unsafe().objectFieldOffset(coderField);
			} catch (Throwable e) {
				_directUserAgeFieldOffset = 0;
			}

			directUserAgeFieldOffset = _directUserAgeFieldOffset;
		}

	}

	private final ReflectTestBean bean = new ReflectTestBean();

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void _do_nothing() {
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int directGetField() throws Exception {
		return bean.directUserAge;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void directSetField() throws Exception {
		bean.directUserAge = 0;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int volatileGetField() throws Exception {
		return bean.volatileUserAge;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void volatileSetField() throws Exception {
		bean.volatileUserAge = 1;
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int reflectFieldGet() throws Exception {
		return directUserAgeField.getInt(bean);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int reflectMethodGet() throws Exception {
		return (int) getDirectUserAgeMethod.invoke(bean, Long.valueOf(1234L));
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int varHandleGet() throws Exception {
		return (int) directUserAgeVarHandle.get(bean);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int directMethodGet() throws Throwable {
		return bean.getDirectUserAge(1234L);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public void directMethodSet() throws Throwable {
		bean.setDirectUserAge(1234L, 18);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int methodHandleGet() throws Throwable {
		return (int) getDirectUserAgeMethodHandle.invoke(bean, Long.valueOf(1234L));
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int methodHandleInvokeWithArgumentsGet() throws Throwable {
		return (int) getDirectUserAgeMethodHandle.invokeWithArguments(new Object[] { bean, Long.valueOf(1234L) });
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int methodHandleInvokeExactGet() throws Throwable {
		return (int) getDirectUserAgeMethodHandle.invokeExact(bean, 1234L);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int unsafeGet() throws Exception {
		return UnsafeUtils.unsafe().getInt(bean, directUserAgeFieldOffset);
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int cglibFastMethodGet() throws Throwable {
		return (int) cgligFastMethod.invoke(bean, new Object[] { 1234L });
	}

	@Benchmark
	@BenchmarkMode({ Mode.Throughput })
	@OutputTimeUnit(TimeUnit.MICROSECONDS)
	public int reflectasmMethodAccessGet() throws Throwable {
		return (int) reflectasmMethodAccess.invoke(bean, reflectasmMethodIndex, 1234L);
	}

	public static void main(String[] args) throws Exception {

		Options opt = new OptionsBuilder()//
				.include(ReflectBenchmark.class.getSimpleName())//
				.warmupIterations(3)//
				.measurementIterations(3)//
				.threads(4)//
				.forks(1)//
				.build();

		new Runner(opt).run();
	}
}
