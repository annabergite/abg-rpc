package rpc.abg.transport.server.rest.protocol;

import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import rpc.abg.invoke.Invoker;

public class RestHttpResponse {
	private Invoker<CompletableFuture<?>> invoker;
	private FullHttpRequest request;
	private HttpResponseStatus status;
	private Object result;
	private boolean keepAlive;

	public RestHttpResponse() {
	}

	public RestHttpResponse(Invoker<CompletableFuture<?>> invoker, FullHttpRequest request, HttpResponseStatus status,
			Object result, boolean keepAlive) {
		this.invoker = invoker;
		this.request = request;
		this.status = status;
		this.result = result;
		this.keepAlive = keepAlive;
	}

	public Invoker<CompletableFuture<?>> getInvoker() {
		return invoker;
	}

	public void setInvoker(Invoker<CompletableFuture<?>> invoker) {
		this.invoker = invoker;
	}

	public FullHttpRequest getRequest() {
		return request;
	}

	public void setRequest(FullHttpRequest request) {
		this.request = request;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public void setStatus(HttpResponseStatus status) {
		this.status = status;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}
}
