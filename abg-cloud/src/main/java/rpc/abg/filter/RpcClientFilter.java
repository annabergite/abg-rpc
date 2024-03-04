package rpc.abg.filter;

import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;

public interface RpcClientFilter {

	public static final String CLIENT_FILTER_DENY = "client filter deny this request";

	/**
	 * 客户端发送前执行
	 * 
	 * @param request
	 *            不为空
	 * 
	 * @return true表示允许发送，false表示不允许发送，不允许发送的情况将执行failover方法
	 */
	boolean onSend(Request request);

	/**
	 * 客户端收到后执行
	 * 
	 * @param request
	 *            不为空
	 * 
	 * @param response
	 *            不为空，为空的情况会调用{@link #onError(Request, Response, Throwable)}
	 */
	void onRecive(Request request, Response response);

	/**
	 * 发生错误时执行
	 * 
	 * @param request
	 *            不为空
	 * 
	 * @param response
	 *            可能为空
	 * 
	 * @param throwable
	 *            不为空
	 */
	void onError(Request request, Response response, Throwable throwable);
}
