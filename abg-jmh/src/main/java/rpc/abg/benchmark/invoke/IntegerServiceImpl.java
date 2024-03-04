package rpc.abg.benchmark.invoke;

public class IntegerServiceImpl implements IntegerService {

	private final Integer value = Integer.valueOf(1);

	@Override
	public Integer getValue() {
		return value;
	}

}
