package rpc.abg.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 速度略慢 115.069 ± 1.193 ops/us
 * 
 * @author Annabergite
 *
 * @param <T>
 *            必须为Weightable子类
 */
public class RandomLoadBalance<T extends Weightable> implements LoadBalance<T> {

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

		int seed = ThreadLocalRandom.current().nextInt(sum + 1);
		return weightableGroup.get(seed);
	}

}
