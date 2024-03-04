package rpc.abg.transport.client.future;

import java.util.concurrent.CompletableFuture;

import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;

public class RequestWithFuture {
	private Request request;
	private CompletableFuture<Response> future;
	private long expireTime;

	public RequestWithFuture(Request request, CompletableFuture<Response> future, long expireTime) {
		this.request = request;
		this.future = future;
		this.expireTime = expireTime;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public CompletableFuture<Response> getFuture() {
		return future;
	}

	public void setFuture(CompletableFuture<Response> future) {
		this.future = future;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

}
