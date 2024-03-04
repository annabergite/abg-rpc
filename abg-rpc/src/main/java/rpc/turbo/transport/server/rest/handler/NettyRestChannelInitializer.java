package rpc.abg.transport.server.rest.handler;

import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import rpc.abg.config.abgConstants;
import rpc.abg.filter.RestServerFilter;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.serialization.JsonMapper;
import rpc.abg.transport.server.rest.codec.RestHttResponseEncoder;

public class NettyRestChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final ServerInvokerFactory invokerFactory;
	private final JsonMapper jsonMapper;
	private final CopyOnWriteArrayList<RestServerFilter> filters;

	public NettyRestChannelInitializer(ServerInvokerFactory invokerFactory, JsonMapper jsonMapper,
			CopyOnWriteArrayList<RestServerFilter> filters) {
		this.invokerFactory = invokerFactory;
		this.jsonMapper = jsonMapper;
		this.filters = filters;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline()//
				.addLast(new HttpServerCodec(1024 * 4, 1024 * 8, 1024 * 16, false))// HTTP 服务的解码器
				.addLast(new HttpObjectAggregator(abgConstants.MAX_FRAME_LENGTH))// HTTP 消息的合并处理
				.addLast(new RestHttResponseEncoder(invokerFactory, jsonMapper, filters))// 自定义编码器
				.addLast(new NettyRestHandler(invokerFactory, jsonMapper, filters)); // 逻辑处理
	}
}
