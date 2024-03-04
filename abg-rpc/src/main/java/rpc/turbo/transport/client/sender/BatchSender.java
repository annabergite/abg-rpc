package rpc.abg.transport.client.sender;

import java.io.IOException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.internal.shaded.org.jctools.queues.atomic.MpscAtomicArrayQueue;
import rpc.abg.transport.client.future.RequestWithFuture;
import rpc.abg.util.FastClearableArrayList;

public class BatchSender implements Sender {
	public static final int MAX_SEND_BUFFER_SIZE = 1024;
	public static final int MAX_SEND_LOOP_COUNT = 16;
	public static final int MAX_BATCH_SIZE = 64;

	private final Channel channel;
	private final ChannelPromise voidPromise;
	private final EventLoop eventLoop;

	private final MpscAtomicArrayQueue<RequestWithFuture> sendBuffer //
			= new MpscAtomicArrayQueue<>(MAX_SEND_BUFFER_SIZE);
	private final FastClearableArrayList<RequestWithFuture> batchList //
			= new FastClearableArrayList<>();

	private final Runnable batchSendTask = () -> doBatchSend();

	public BatchSender(Channel channel) {
		this.channel = channel;
		this.voidPromise = channel.voidPromise();
		this.eventLoop = channel.eventLoop();
	}

	@Override
	public void send(RequestWithFuture request) {
		while (!sendBuffer.offer(request)) {
			// 已经满了，必须要清理
			eventLoop.execute(batchSendTask);
		}

		if (!sendBuffer.isEmpty()) {
			eventLoop.execute(batchSendTask);
		}
	}

	private void doBatchSend() {
		if (sendBuffer.isEmpty()) {
			return;
		}

		for (int r = 0; r < MAX_SEND_LOOP_COUNT; r++) {
			for (int i = 0; i < MAX_BATCH_SIZE; i++) {
				RequestWithFuture request = sendBuffer.poll();

				if (request != null) {
					batchList.add(request);
				} else {
					break;
				}
			}

			if (!batchList.isEmpty()) {
				channel.write(batchList, voidPromise);
				batchList.clear();
			}

			if (sendBuffer.isEmpty()) {
				break;
			}
		}

		channel.flush();
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

}
