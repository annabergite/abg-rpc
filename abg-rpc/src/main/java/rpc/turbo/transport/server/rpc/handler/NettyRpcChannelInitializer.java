package rpc.abg.transport.server.rpc.handler;

import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import rpc.abg.config.abgConstants;
import rpc.abg.filter.RpcServerFilter;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.serialization.Serializer;
import rpc.abg.transport.server.rpc.codec.RequestDecoder;
import rpc.abg.transport.server.rpc.codec.ResponseEncoder;

public class NettyRpcChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final ServerInvokerFactory invokerFactory;
	private final Serializer serializer;
	private final CopyOnWriteArrayList<RpcServerFilter> filters;

	public NettyRpcChannelInitializer(ServerInvokerFactory invokerFactory, Serializer serializer,
			CopyOnWriteArrayList<RpcServerFilter> filters) {
		this.invokerFactory = invokerFactory;
		this.serializer = serializer;
		this.filters = filters;

	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline()//
				.addLast("encoder", new ResponseEncoder(serializer))//
				.addLast("decoder", new RequestDecoder(abgConstants.MAX_FRAME_LENGTH, serializer))//
				.addLast("handler", new NettyRpcServerHandler(invokerFactory, filters));
	}
}
