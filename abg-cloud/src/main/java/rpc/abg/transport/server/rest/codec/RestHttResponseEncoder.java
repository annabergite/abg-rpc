package rpc.abg.transport.server.rest.codec;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import rpc.abg.common.RemoteContext;
import rpc.abg.config.HostPort;
import rpc.abg.filter.RestServerFilter;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.serialization.JsonMapper;
import rpc.abg.transport.server.rest.protocol.RestHttpResponse;
import rpc.abg.util.UnsafeStringUtils;

public class RestHttResponseEncoder extends ChannelOutboundHandlerAdapter {
	private static final Log logger = LogFactory.getLog(RestHttResponseEncoder.class);

	private final ServerInvokerFactory invokerFactory;
	private final JsonMapper jsonMapper;
	private final CopyOnWriteArrayList<RestServerFilter> filters;
	private HostPort clientAddress;
	private HostPort serverAddress;

	public RestHttResponseEncoder(ServerInvokerFactory invokerFactory, JsonMapper jsonMapper,
			CopyOnWriteArrayList<RestServerFilter> filters) {
		this.invokerFactory = invokerFactory;
		this.jsonMapper = jsonMapper;
		this.filters = filters;
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (!(msg instanceof RestHttpResponse)) {
			ctx.write(msg, promise);
			return;
		}

		RestHttpResponse restHttpResponse = (RestHttpResponse) msg;
		HttpResponseStatus status = restHttpResponse.getStatus();

		if (status == null) {
			status = INTERNAL_SERVER_ERROR;
		}

		if (restHttpResponse.getResult() == null) {
			status = INTERNAL_SERVER_ERROR;
			restHttpResponse.setStatus(status);
			restHttpResponse.setResult("UNKNOWN");
		}

		doResponse(ctx, promise, restHttpResponse);
	}

	private void doResponse(ChannelHandlerContext ctx, ChannelPromise promise, RestHttpResponse restHttpResponse) {
		ByteBuf buffer = ctx.alloc().ioBuffer();

		Object msg = restHttpResponse.getResult();
		HttpResponseStatus status = restHttpResponse.getStatus();
		boolean keepAlive = restHttpResponse.isKeepAlive();
		Throwable throwable = null;

		if (msg == null) {
			buffer.writeBytes(UnsafeStringUtils.getUTF8Bytes(""));
		} else if (msg instanceof Throwable) {
			throwable = (Throwable) msg;
			buffer.writeBytes(UnsafeStringUtils.getUTF8Bytes(throwable.getMessage()));
		} else {
			try {
				jsonMapper.write(buffer, msg);

				FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer, false);
				response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
			} catch (Throwable e) {
				if (logger.isWarnEnabled()) {
					logger.warn("error ", e);
				}

				status = INTERNAL_SERVER_ERROR;

				buffer.clear();
				buffer.writeBytes(UnsafeStringUtils.getUTF8Bytes(e.getMessage()));
			}
		}

		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer, false);
		response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

		doResponseFilter(ctx, restHttpResponse.getRequest(), response, restHttpResponse.getInvoker(), throwable);

		if (keepAlive) {
			response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			ctx.write(response, promise);
		} else {
			ctx.write(response, promise).addListener(CLOSE);
		}
	}

	private void doResponseFilter(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response,
			Invoker<CompletableFuture<?>> invoker, Throwable throwable) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return;
		}

		if (clientAddress == null) {
			InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
			clientAddress = new HostPort(insocket.getAddress().getHostAddress(), 0);
		}

		if (serverAddress == null) {
			InetSocketAddress insocket = (InetSocketAddress) ctx.channel().localAddress();
			serverAddress = new HostPort(insocket.getAddress().getHostAddress(), insocket.getPort());
		}

		RemoteContext.setServerAddress(serverAddress);
		RemoteContext.setClientAddress(clientAddress);

		if (invoker != null) {
			RemoteContext.setRemoteMethod(invoker.getMethod());
			RemoteContext.setServiceMethodName(invokerFactory.getServiceMethodName(invoker.getServiceId()));
		} else {
			RemoteContext.setRemoteMethod(null);
			RemoteContext.setServiceMethodName(null);
		}

		if (response.status() == HttpResponseStatus.OK) {
			for (int i = 0; i < filterLength; i++) {
				RestServerFilter filter = filters.get(i);
				filter.onSend(request, response);
			}
		} else {
			for (int i = 0; i < filterLength; i++) {
				RestServerFilter filter = filters.get(i);
				filter.onError(request, response, throwable);
			}
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)).addListener(CLOSE);
	}

}
