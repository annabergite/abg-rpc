package rpc.abg.boot;

import org.springframework.beans.factory.Aware;

import rpc.abg.server.abgServer;

/**
 * abgServer切入点, 实现类必须是被spring管理的
 */
public interface abgServerAware extends Aware {

	/**
	 * 初始化完成后调用
	 * 
	 * @param abgServer
	 */
	void setabgServer(abgServer abgServer);

}
