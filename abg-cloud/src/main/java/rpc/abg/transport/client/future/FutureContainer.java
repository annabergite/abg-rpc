package rpc.abg.transport.client.future;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap.PrimitiveEntry;
import rpc.abg.protocol.Response;
import rpc.abg.transport.client.exception.ConnectionException;
import rpc.abg.transport.client.exception.ResponseTimeoutException;
import rpc.abg.util.SystemClock;

/**
 * 
 * @author Annabergite
 *
 */
public final class FutureContainer implements Closeable {

	private final IntObjectHashMap<RequestWithFuture> futureMap = //
			new IntObjectHashMap<>();

	public void add(RequestWithFuture requestWithFuture) {
		if (requestWithFuture.getFuture().isDone()) {
			return;
		}

		futureMap.put(requestWithFuture.getRequest().getRequestId(), requestWithFuture);
	}

	public void remove(int requestId) {
		futureMap.remove(requestId);
	}

	public void expire(int requestId) {
		RequestWithFuture requestWithFuture = futureMap.remove(requestId);

		if (requestWithFuture == null) {
			return;
		}

		CompletableFuture<Response> future = requestWithFuture.getFuture();

		if (!future.isDone()) {
			future.completeExceptionally(ResponseTimeoutException.NONE_STACK_TRACE);
		}
	}

	public void notifyResponse(Response response) {
		if (response == null) {
			return;
		}

		RequestWithFuture requestWithFuture = futureMap.remove(response.getRequestId());

		if (requestWithFuture == null) {
			return;
		}

		CompletableFuture<Response> future = requestWithFuture.getFuture();

		if (!future.isDone()) {
			future.complete(response);
		}
	}

	/**
	 * 删除过期任务
	 * 
	 * @param maxTime
	 *            毫秒，超时则跳出
	 */
	public void doExpireJob(long maxTime) {
		long finishTime = SystemClock.fast().mills() + maxTime;

		Iterator<PrimitiveEntry<RequestWithFuture>> iterator//
				= futureMap.entries().iterator();

		while (iterator.hasNext()) {
			RequestWithFuture requestWithFuture = iterator.next().value();

			long now = SystemClock.fast().mills();

			// 防止执行过长时间
			if (now > finishTime) {
				break;
			}

			if (now < requestWithFuture.getExpireTime()) {
				continue;
			}

			iterator.remove();

			CompletableFuture<Response> future = requestWithFuture.getFuture();

			if (future.isDone()) {
				return;
			} else {
				future.completeExceptionally(ResponseTimeoutException.NONE_STACK_TRACE);
			}
		}
	}

	/**
	 * 会尝试平滑退出, 不会实际抛出异常
	 */
	@Override
	public void close() throws IOException {
		if (futureMap.isEmpty()) {
			return;
		}

		doExpireJob(10);

		if (futureMap.isEmpty()) {
			return;
		}

		Iterator<PrimitiveEntry<RequestWithFuture>> iterator//
				= futureMap.entries().iterator();

		while (iterator.hasNext()) {
			RequestWithFuture requestWithFuture = iterator.next().value();

			iterator.remove();

			requestWithFuture//
					.getFuture()//
					.completeExceptionally(new ConnectionException("connection is closed"));
		}
	}
}