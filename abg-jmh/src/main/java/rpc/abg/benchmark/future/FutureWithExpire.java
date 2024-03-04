package rpc.abg.benchmark.future;

import java.util.concurrent.CompletableFuture;

public final class FutureWithExpire<T> {
	public final CompletableFuture<T> future;
	public final long expireTime;

	public FutureWithExpire(CompletableFuture<T> future, long expireTime) {
		this.future = future;
		this.expireTime = expireTime;
	}
}