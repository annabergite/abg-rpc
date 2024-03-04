package rpc.abg.loadbalance;

import java.util.List;

import rpc.abg.util.concurrent.ConcurrentIntegerSequencer;

/**
 * 速度最快 204.642 ± 1.959 ops/us
 * 
 * @author Annabergite
 *
 * @param <T>
 *            必须为Weightable子类
 */
public class RoundRobinLoadBalance<T extends Weightable> implements LoadBalance<T> {
	private final ConcurrentIntegerSequencer sequencer = new ConcurrentIntegerSequencer(0, true);

	protected volatile WeightableGroup<T> weightableGroup = null;

	@Override
	public void setWeightables(List<T> weightables) {
		weightableGroup = new WeightableGroup<>(weightables);
	}

	@Override
	public T select() {
		final WeightableGroup<T> weightableGroup = this.weightableGroup;

		if (weightableGroup == null) {
			return null;
		}

		int sum = weightableGroup.sum();

		if (sum < 2) {
			return weightableGroup.get(0);
		}

		int seed = sequencer.next();
		return weightableGroup.get(seed);
	}

}
