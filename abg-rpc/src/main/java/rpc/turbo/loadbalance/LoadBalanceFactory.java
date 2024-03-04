package rpc.abg.loadbalance;

/**
 * 
 * @author Annabergite
 *
 * @param <T>
 *            必须为Weightable子类
 */
public interface LoadBalanceFactory<T extends Weightable> {

	/**
	 * 创建一个新的LoadBalance
	 * 
	 * @return
	 */
	LoadBalance<T> newLoadBalance();
}
