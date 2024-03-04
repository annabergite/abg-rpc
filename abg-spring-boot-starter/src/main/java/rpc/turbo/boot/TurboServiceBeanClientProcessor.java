package rpc.abg.boot;

import org.springframework.beans.BeansException;

/**
 * 拦截并处理abgService远程调用实例, 实现类必须是被spring管理的
 * 
 * @author Annabergite
 *
 */
public interface abgServiceBeanClientProcessor {

	/**
	 * 对创建的abgService远程调用实例进行处理
	 * 
	 * @param serviceClass
	 * @param serviceBean
	 * @return
	 * @throws BeansException
	 */
	public Object process(Class<?> serviceClass, Object serviceBean) throws BeansException;

}
