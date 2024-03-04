package rpc.abg.loadbalance;

public class RandomLoadBalanceFactory<T extends Weightable> implements LoadBalanceFactory<T> {

	@Override
	public LoadBalance<T> newLoadBalance() {
		return new RandomLoadBalance<>();
	}

}
