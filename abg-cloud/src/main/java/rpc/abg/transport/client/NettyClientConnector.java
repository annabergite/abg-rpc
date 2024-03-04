package rpc.abg.transport.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import rpc.abg.config.HostPort;
import rpc.abg.serialization.Serializer;
import rpc.abg.transport.client.future.RequestWithFuture;
import rpc.abg.transport.client.handler.abgChannelInitializer;
import rpc.abg.transport.client.sender.BatchSender;
import rpc.abg.transport.client.sender.Sender;

final class NettyClientConnector implements Closeable {
	private static final Log logger = LogFactory.getLog(NettyClientConnector.class);

	public static final int MAX_SEND_BUFFER_SIZE = 1024;

	public final HostPort serverAddress;

	private final Serializer serializer;
	private final EventLoopGroup eventLoopGroup;
	private final int connectCount;

	public volatile HostPort clientAddress;
	private volatile Sender[] senders;

	/**
	 * 
	 * @param eventLoopGroup
	 * @param serializer
	 * @param futureContainer
	 * @param serverAddress
	 * @param connectCount
	 */
	NettyClientConnector(EventLoopGroup eventLoopGroup, //
			Serializer serializer, //
			HostPort serverAddress, int connectCount) {
		this.eventLoopGroup = eventLoopGroup;
		this.connectCount = connectCount;
		this.serverAddress = serverAddress;
		this.serializer = serializer;
	}

	int connectCount() {
		return connectCount;
	}

	/**
	 * 
	 * @param channelIndex
	 *            发送数据的channel
	 * 
	 * @param requestWithFuture
	 *            请求数据
	 */
	void send(int channelIndex, RequestWithFuture requestWithFuture) {
		Objects.requireNonNull(requestWithFuture, "request is null");
		senders[channelIndex].send(requestWithFuture);
	}

	void connect() throws InterruptedException {

		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(eventLoopGroup);

		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.SO_RCVBUF, 256 * 1024);
		bootstrap.option(ChannelOption.SO_SNDBUF, 256 * 1024);
		bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, //
				new WriteBufferWaterMark(1024 * 1024, 2048 * 1024));

		if (eventLoopGroup instanceof EpollEventLoopGroup) {
			bootstrap.option(EpollChannelOption.SO_REUSEPORT, true);
			bootstrap.channel(EpollSocketChannel.class);
		} else if (eventLoopGroup instanceof NioEventLoopGroup) {
			bootstrap.channel(NioSocketChannel.class);
		}

		bootstrap.handler(new abgChannelInitializer(serializer));

		Sender[] newSenders = new Sender[connectCount];
		for (int i = 0; i < connectCount; i++) {
			Channel channel = bootstrap.connect(serverAddress.host, serverAddress.port).sync().channel();
			newSenders[i] = new BatchSender(channel);

			if (logger.isInfoEnabled()) {
				logger.info(serverAddress + " connect " + i + "/" + connectCount);
			}

			if (i == 0) {
				InetSocketAddress insocket = (InetSocketAddress) channel.localAddress();
				clientAddress = new HostPort(insocket.getAddress().getHostAddress(), 0);
			}
		}

		Sender[] old = senders;
		senders = newSenders;

		if (old != null) {
			for (int i = 0; i < old.length; i++) {
				try {
					old[i].close();
				} catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("关闭出错", e);
					}
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (senders == null) {
			return;
		}

		final Sender[] senders = this.senders;
		this.senders = null;

		for (int i = 0; i < senders.length; i++) {
			try {
				senders[i].close();
			} catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("关闭出错", e);
				}
			}
		}
	}

}
