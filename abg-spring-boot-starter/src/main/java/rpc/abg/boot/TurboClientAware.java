package rpc.abg.boot;

import org.springframework.beans.factory.Aware;

import rpc.abg.client.abgClient;

/**
 * abgClient切入点, 实现类必须是被spring管理的
 */
public interface abgClientAware extends Aware {

	/**
	 * 初始化完成后调用
	 * 
	 * @param abgClient
	 */
	void setabgClient(abgClient abgClient);

}
