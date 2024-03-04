package rpc.abg.transport.client.sender;

import java.io.IOException;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import rpc.abg.transport.client.future.RequestWithFuture;

public class SingleSender implements Sender {
	private final Channel channel;
	private final ChannelPromise voidPromise;

	public SingleSender(Channel channel) {
		this.channel = channel;
		this.voidPromise = channel.voidPromise();
	}

	@Override
	public void send(RequestWithFuture request) {
		channel.writeAndFlush(request, voidPromise);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

}
