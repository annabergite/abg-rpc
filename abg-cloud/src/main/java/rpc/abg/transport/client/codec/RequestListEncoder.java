package rpc.abg.transport.client.codec;

import static rpc.abg.config.abgConstants.EXPIRE_PERIOD;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;
import rpc.abg.serialization.Serializer;
import rpc.abg.transport.client.future.FutureContainer;
import rpc.abg.transport.client.future.RequestWithFuture;

public class RequestListEncoder extends MessageToByteEncoder<List<RequestWithFuture>> {
	private static final Log logger = LogFactory.getLog(RequestListEncoder.class);

	private final Serializer serializer;
	private final FutureContainer futureContainer;

	public RequestListEncoder(Serializer serializer, FutureContainer futureContainer) {
		this.serializer = serializer;
		this.futureContainer = futureContainer;
	}

	protected void encode(ChannelHandlerContext ctx, List<RequestWithFuture> requestList, ByteBuf buffer)
			throws Exception {
		if (requestList instanceof RandomAccess) {
			for (int i = 0; i < requestList.size(); i++) {
				doEncode(buffer, requestList.get(i));
			}
		} else {
			for (RequestWithFuture request : requestList) {
				doEncode(buffer, request);
			}
		}
	}

	private void doEncode(ByteBuf buffer, RequestWithFuture request) throws IOException {
		futureContainer.add(request);
		serializer.writeRequest(buffer, request.getRequest());

		request.setRequest(null);// help to gc
	}

	@Override
	public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
			ChannelPromise promise) throws Exception {
		super.connect(ctx, remoteAddress, localAddress, promise);

		if (ctx.channel().attr(CodecConstants.STARTED_AUTO_EXPIRE_JOB).compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
			ctx.executor().scheduleAtFixedRate(//
					() -> futureContainer.doExpireJob(1), //
					EXPIRE_PERIOD, EXPIRE_PERIOD, TimeUnit.MILLISECONDS);

			if (logger.isInfoEnabled()) {
				logger.info("FutureContainer startingAutoExpireJob");
			}
		}

		if (logger.isInfoEnabled()) {
			logger.info("channel connect: " + ctx.channel());
		}
	}

	@Override
	public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		super.disconnect(ctx, promise);

		if (logger.isInfoEnabled()) {
			logger.info("channel disconnect: " + ctx.channel());
		}
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		super.close(ctx, promise);

		futureContainer.close();

		if (logger.isInfoEnabled()) {
			logger.info("channel close: " + ctx.channel());
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.channel().close();

		futureContainer.close();
	}
}
