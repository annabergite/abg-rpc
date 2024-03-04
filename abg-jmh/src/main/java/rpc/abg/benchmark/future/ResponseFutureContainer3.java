package rpc.abg.benchmark.future;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import rpc.abg.annotation.AbgService;
import rpc.abg.protocol.Response;
import rpc.abg.transport.client.exception.ConnectionException;
import rpc.abg.util.SystemClock;

/**
 * 
 * @author Annabergite
 *
 */
public final class ResponseFutureContainer3 implements Closeable {
	private volatile boolean isClosing = false;

	private static final int SEGMENT = 64;

	private final ConcurrentHashMap<Integer, FutureWithExpire<Response>>[] futureMapArray;

	@SuppressWarnings("unchecked")
	public ResponseFutureContainer3() {
		futureMapArray = new ConcurrentHashMap[SEGMENT];

		for (int i = 0; i < SEGMENT; i++) {
			futureMapArray[i] = new ConcurrentHashMap<>();
		}
	}

	public void addFuture(int requestId, CompletableFuture<Response> future) {
		addFuture(requestId, future, AbgService.DEFAULT_TIME_OUT);
	}

	private ConcurrentHashMap<Integer, FutureWithExpire<Response>> futureMap(int requestId) {
		int index = requestId & (SEGMENT - 1);
		return futureMapArray[index];
	}

	public void addFuture(int requestId, CompletableFuture<Response> future, long timeout) {
		if (future.isDone()) {
			return;
		}

		if (isClosing) {
			throw new ConnectionException("it's closed");
		}

		long expireTime = timeout + SystemClock.fast().mills();

		futureMap(requestId).put(requestId, new FutureWithExpire<>(future, expireTime));
	}

	public void remove(int requestId) {
		futureMap(requestId).remove(requestId);
	}

	public void notifyResponse(Response response) {
		if (response == null) {
			return;
		}

		int requestId = response.getRequestId();
		FutureWithExpire<Response> futureWithExpire = futureMap(requestId).remove(requestId);

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

	}

	/**
	 * 会尝试平滑退出, 不会实际抛出异常
	 */
	@Override
	public void close() throws IOException {
		// 尝试平滑退出
		for (int i = 0; i < AbgService.DEFAULT_TIME_OUT; i += 100) {
			doExpireJob();

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				break;
			}
		}

		isClosing = true;
	}
}