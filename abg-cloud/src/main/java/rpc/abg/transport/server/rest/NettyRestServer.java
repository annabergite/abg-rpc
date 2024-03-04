package rpc.abg.transport.server.rest;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import rpc.abg.config.HostPort;
import rpc.abg.filter.RestServerFilter;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.serialization.JsonMapper;
import rpc.abg.transport.server.rest.handler.NettyRestChannelInitializer;

public class NettyRestServer implements Closeable {

	private final HostPort hostPort;
	private final EventLoopGroup eventLoopGroup;
	private final ServerInvokerFactory invokerFactory;
	private volatile Channel channel;
	private final JsonMapper jsonMapper;
	private final CopyOnWriteArrayList<RestServerFilter> filters;

	public NettyRestServer(EventLoopGroup eventLoopGroup, ServerInvokerFactory invokerFactory, JsonMapper jsonMapper,
			CopyOnWriteArrayList<RestServerFilter> filters, HostPort hostPort) {
		this.eventLoopGroup = eventLoopGroup;
		this.invokerFactory = invokerFactory;
		this.hostPort = hostPort;
		this.jsonMapper = jsonMapper;
		this.filters = filters;
	}

	public void start() throws InterruptedException {
		InetSocketAddress inet = new InetSocketAddress(hostPort.host, hostPort.port);

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(eventLoopGroup);

		bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.SO_RCVBUF, 256 * 1024);

		if (eventLoopGroup instanceof EpollEventLoopGroup) {
			bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
			bootstrap.channel(EpollServerSocketChannel.class);
		} else if (eventLoopGroup instanceof NioEventLoopGroup) {
			bootstrap.channel(NioServerSocketChannel.class);
		}

		bootstrap.childHandler(new NettyRestChannelInitializer(invokerFactory, jsonMapper, filters));

		bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
		bootstrap.childOption(ChannelOption.SO_RCVBUF, 256 * 1024);
		bootstrap.childOption(ChannelOption.SO_SNDBUF, 256 * 1024);
		bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, //
				new WriteBufferWaterMark(1024 * 1024, 2048 * 1024));

		channel = bootstrap.bind(inet).sync().channel();

		System.out.println("NettyRestServer started. Listening on: " + hostPort);
	}

	@Override
	public void close() throws IOException {
		try {
			channel.closeFuture().sync();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}
}