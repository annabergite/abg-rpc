package rpc.abg.loadbalance;

import java.util.List;

/**
 * 负载均衡，有状态的，线程安全的
 * 
 * @author Annabergite
 *
 * @param <T>
 *            必须为Weightable子类
 * 
 */
public interface LoadBalance<T extends Weightable> {

	/**
	 * 可多次重复设置
	 * 
	 * @param weightables
	 */
	void setWeightables(List<T> weightables);

	/**
	 * 选出一个来
	 * 
	 * @return
	 */
	T select();
}
