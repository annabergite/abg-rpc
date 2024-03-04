package rpc.abg.transport.client.handler;

import static rpc.abg.config.abgConstants.MAX_FRAME_LENGTH;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import rpc.abg.serialization.Serializer;
import rpc.abg.transport.client.codec.RequestEncoder;
import rpc.abg.transport.client.codec.RequestListEncoder;
import rpc.abg.transport.client.codec.ResponseDecoder;
import rpc.abg.transport.client.future.FutureContainer;

public class abgChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final Serializer serializer;

	public abgChannelInitializer(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		FutureContainer container = new FutureContainer();

		RequestEncoder requestEncoder = new RequestEncoder(serializer, container);
		RequestListEncoder requestListEncoder = new RequestListEncoder(serializer, container);
		ResponseDecoder decoder = new ResponseDecoder(MAX_FRAME_LENGTH, serializer, container);

		ch.pipeline()//
				.addLast("requestEncoder", requestEncoder)//
				.addLast("requestListEncoder", requestListEncoder)//
				.addLast("decoder", decoder);
	}
}
