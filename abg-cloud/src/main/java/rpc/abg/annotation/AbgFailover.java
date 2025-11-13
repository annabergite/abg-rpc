/**
 * 
 */
package rpc.abg.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 远程调用失败时的回退实现，<br>
 * 松散约束可以不实现服务接口，只要方法签名一致就能自动匹配上,<br>
 * 优先于RPCService接口中的默认方法
 * 
 * @author Annabergite
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
public @interface AbgFailover {
	/**
	 * 服务接口
	 * 
	 * @return
	 */
	Class<?> service();
}
