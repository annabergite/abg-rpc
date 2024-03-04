package rpc.abg.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;

/**
 * jvm实例共享EventLoopGroup
 * 
 * @author Annabergite
 *
 */
public class EventLoopGroupHolder {
	private static final Log logger = LogFactory.getLog(EventLoopGroupHolder.class);

	public static final int NIO_THREAD_COUNT = NettyRuntime.availableProcessors() * 2;

	private static int reference = 0;
	private static EventLoopGroup eventLoopGroup;

	static {
		if (Epoll.isAvailable()) {
			eventLoopGroup = new EpollEventLoopGroup(NIO_THREAD_COUNT);
			((EpollEventLoopGroup) eventLoopGroup).setIoRatio(99);
		} else {
			eventLoopGroup = new NioEventLoopGroup(NIO_THREAD_COUNT);
			((NioEventLoopGroup) eventLoopGroup).setIoRatio(99);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> close(), "eventLoopGroup-close-thread"));
	}

	/**
	 * 获取EventLoopGroup， 使用完毕必须执行{@link #release(EventLoopGroup)}
	 * 
	 * @return
	 */
	public synchronized static EventLoopGroup get() {
		if (eventLoopGroup == null) {
			throw new RuntimeException("eventLoopGroup has been closed");
		}

		++reference;
		return eventLoopGroup;
	}

	/**
	 * 释放eventLoopGroup，内部使用引用计数，当计数为0时实际关闭
	 * 
	 * @param eventLoopGroup
	 */
	public synchronized static void release(EventLoopGroup eventLoopGroup) {
		if (--reference == 0) {
			close();
		}
	}

	private synchronized static void close() {
		if (eventLoopGroup == null) {
			return;
		}

		try {
			eventLoopGroup.shutdownGracefully().syncUninterruptibly();

			if (logger.isInfoEnabled()) {
				logger.info("成功关闭 eventLoopGroup");
			}
		} catch (Throwable t) {
			if (logger.isWarnEnabled()) {
				logger.warn("eventLoopGroup shutdown error", t);
			}
		}

		eventLoopGroup = null;
	}
}
