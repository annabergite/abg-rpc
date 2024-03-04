package rpc.abg.benchmark.future;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.jctools.maps.NonBlockingHashMapLong;

import rpc.abg.annotation.AbgService;
import rpc.abg.protocol.Response;
import rpc.abg.transport.client.exception.ConnectionException;
import rpc.abg.transport.client.exception.ResponseTimeoutException;
import rpc.abg.util.SystemClock;

/**
 * 
 * @author Annabergite
 *
 */
public final class ResponseFutureContainer1 implements Closeable {
	private volatile boolean isClosing = false;

	private final NonBlockingHashMapLong<FutureWithExpire<Response>> futureMap = //
			new NonBlockingHashMapLong<>(32);

	public void addFuture(int requestId, CompletableFuture<Response> future) {
		addFuture(requestId, future, AbgService.DEFAULT_TIME_OUT);
	}

	public void addFuture(int requestId, CompletableFuture<Response> future, long timeout) {
		if (future.isDone()) {
			return;
		}

		if (isClosing) {
			throw new ConnectionException("it's closed");
		}

		long expireTime = timeout + SystemClock.fast().mills();

		futureMap.put(requestId, new FutureWithExpire<>(future, expireTime));
	}

	public void remove(int requestId) {
		futureMap.remove(requestId);
	}

	public void notifyResponse(Response response) {
		if (response == null) {
			return;
		}

		FutureWithExpire<Response> futureWithExpire = futureMap.remove(response.getRequestId());

		if (futureWithExpire == null) {
			return;
		}

		futureWithExpire.future.complete(response);
	}

	/**
	 * 外部线程周期性调用
	 */
	public void doExpireJob() {
		if (isClosing) {
			return;
		}

		futureMap.forEach((key, value) -> {
			doExpire(key, value);
		});
	}

	private void doExpire(long requestId, FutureWithExpire<Response> futureWithExpire) {
		CompletableFuture<Response> future = futureWithExpire.future;

		if (future.isDone()) {
			return;
		}

		long currentTime = SystemClock.fast().mills();
		if (futureWithExpire.expireTime > currentTime) {
			return;
		}

		future.completeExceptionally(ResponseTimeoutException.NONE_STACK_TRACE);

		futureMap.remove(requestId);
	}

	/**
	 * 会尝试平滑退出, 不会实际抛出异常
	 */
	@Override
	public void close() throws IOException {
		// 尝试平滑退出
		for (int i = 0; i < AbgService.DEFAULT_TIME_OUT; i += 100) {
			doExpireJob();

			if (futureMap.isEmpty()) {
				break;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}

		isClosing = true;
	}
}