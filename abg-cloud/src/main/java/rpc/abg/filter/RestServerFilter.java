package rpc.abg.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public interface RestServerFilter {

	public static final String SERVER_FILTER_DENY = "rest server filter deny this request";

	/**
	 * 服务端收到请求后执行
	 * 
	 * @param request
	 *            不为空
	 * 
	 * @return true表示允许发送，false表示不允许发送，<br>
	 *         不允许发送的情况将返回"rest server filter deny this request"给客户端
	 */
	boolean onRecive(FullHttpRequest request);

	/**
	 * 服务端发送请求前执行
	 * 
	 * @param request
	 *            不为空
	 * 
	 * @param response
	 *            不为空
	 */
	void onSend(FullHttpRequest request, FullHttpResponse response);

	/**
	 * 发生错误时执行
	 * 
	 * @param request
	 *            可能为空
	 * 
	 * @param response
	 *            可能为空
	 * 
	 * @param throwable
	 *            不为空
	 */
	void onError(FullHttpRequest request, FullHttpResponse response, Throwable throwable);

}
