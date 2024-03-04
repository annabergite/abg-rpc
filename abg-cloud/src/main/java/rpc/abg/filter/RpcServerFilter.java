package rpc.abg.filter;

import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;

public interface RpcServerFilter {

	public static final String SERVER_FILTER_DENY = "rpc server filter deny this request";

	/**
	 * 服务端收到请求后执行
	 * 
	 * @param request
	 *            不为空，为空的情况会调用{@link #onError(Request, Response, Throwable)}
	 * 
	 * @return true表示允许发送，false表示不允许发送，<br>
	 *         不允许发送的情况将返回"rpc server filter deny this request"给客户端
	 */
	boolean onRecive(Request request);

	/**
	 * 服务端发送请求前执行
	 * 
	 * @param request
	 *            不为空，为空的情况会调用{@link #onError(Request, Response, Throwable)}
	 * 
	 * @param response
	 *            不为空，为空的情况会调用{@link #onError(Request, Response, Throwable)}
	 */
	void onSend(Request request, Response response);

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
	void onError(Request request, Response response, Throwable throwable);
}
