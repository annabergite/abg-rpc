package rpc.abg.transport.server.rest.handler;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import rpc.abg.common.RemoteContext;
import rpc.abg.config.HostPort;
import rpc.abg.filter.RestServerFilter;
import rpc.abg.invoke.InvokeException;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.param.HttpParamExtractor;
import rpc.abg.param.MethodParam;
import rpc.abg.serialization.JsonMapper;
import rpc.abg.transport.server.rest.protocol.RestHttpResponse;

public class NettyRestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Log logger = LogFactory.getLog(NettyRestHandler.class);

	private static final Throwable ONLY_SUPPORT_GET_POST = new InvokeException("only support get and post", false);
	private static final Throwable NOT_SUPPORT_THIS_METHOD = new InvokeException("not support this method", false);
	private static final Throwable UNKNOWN = new InvokeException("UNKNOWN ERROR", false);

	private final ServerInvokerFactory invokerFactory;
	private final JsonMapper jsonMapper;
	private final CopyOnWriteArrayList<RestServerFilter> filters;
	private HostPort clientAddress;
	private HostPort serverAddress;

	public NettyRestHandler(ServerInvokerFactory invokerFactory, JsonMapper jsonMapper,
			CopyOnWriteArrayList<RestServerFilter> filters) {
		this.invokerFactory = invokerFactory;
		this.jsonMapper = jsonMapper;
		this.filters = filters;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);

		if (logger.isInfoEnabled()) {
			logger.info("channelActive: " + ctx.channel());
		}

		InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
		clientAddress = new HostPort(insocket.getAddress().getHostAddress(), 0);

		insocket = (InetSocketAddress) ctx.channel().localAddress();
		serverAddress = new HostPort(insocket.getAddress().getHostAddress(), insocket.getPort());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.channel().close();
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, final FullHttpRequest httpRequest) throws Exception {

		boolean keepAlive = HttpUtil.isKeepAlive(httpRequest);

		String uri = httpRequest.uri();
		HttpMethod httpMethod = httpRequest.method();

		int index = uri.indexOf('&', invokerFactory.restPrefix.length());
		if (index < 0) {
			index = uri.length();
		}

		if (invokerFactory.restPrefix.length() >= index) {
			if (logger.isInfoEnabled()) {
				logger.info("not support this method " + toString(httpRequest));
			}

			doRequestFilter(httpRequest, null);

			ctx.write(new RestHttpResponse(null, httpRequest, NOT_FOUND, NOT_SUPPORT_THIS_METHOD, keepAlive),
					ctx.voidPromise());
			return;
		}

		String restPath = uri.substring(invokerFactory.restPrefix.length(), index);
		final Invoker<CompletableFuture<?>> invoker = invokerFactory.get(restPath);
		CompletableFuture<?> future = null;

		try {
			if (invoker == null) {
				if (logger.isInfoEnabled()) {
					logger.info("not support this method " + toString(httpRequest));
				}

				doRequestFilter(httpRequest, null);

				ctx.write(new RestHttpResponse(null, httpRequest, NOT_FOUND, NOT_SUPPORT_THIS_METHOD, keepAlive),
						ctx.voidPromise());
				return;
			}

			boolean allowHandle = doRequestFilter(httpRequest, invoker);

			if (!allowHandle) {
				ctx.write(new RestHttpResponse(invoker, httpRequest, SERVICE_UNAVAILABLE,
						RestServerFilter.SERVER_FILTER_DENY, keepAlive), ctx.voidPromise());

				return;
			}

			Object params = null;
			if (httpMethod == HttpMethod.GET) {
				params = HttpParamExtractor.extractFromQueryPath(invoker, uri, index);
			} else if (httpMethod == HttpMethod.POST) {
				params = HttpParamExtractor.extractFromBody(invoker, jsonMapper, httpRequest.content());
			} else {
				if (logger.isInfoEnabled()) {
					logger.info("only support get and post " + toString(httpRequest));
				}

				ctx.write(new RestHttpResponse(invoker, httpRequest, INTERNAL_SERVER_ERROR, ONLY_SUPPORT_GET_POST,
						keepAlive), ctx.voidPromise());
				return;
			}

			if (params == null) {
				future = invoker.invoke();
			} else if (params instanceof MethodParam) {
				future = invoker.invoke((MethodParam) params);
			} else if (params instanceof Object[]) {
				future = invoker.invoke((Object[]) params);
			} else {
				future = invoker.invoke((Object) params);
			}
		} catch (Throwable e) {
			if (logger.isWarnEnabled()) {
				logger.warn(uri + " error ", e);
			}

			ctx.write(new RestHttpResponse(invoker, httpRequest, INTERNAL_SERVER_ERROR, e, keepAlive),
					ctx.voidPromise());
			return;
		}

		if (future == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("unknown error " + toString(httpRequest));
			}

			ctx.write(new RestHttpResponse(invoker, httpRequest, INTERNAL_SERVER_ERROR, UNKNOWN, keepAlive),
					ctx.voidPromise());
			return;
		}

		future.whenComplete((result, throwable) -> {
			if (result != null) {
				ctx.write(new RestHttpResponse(invoker, httpRequest, OK, result, keepAlive), ctx.voidPromise());
			} else if (throwable != null) {
				ctx.write(new RestHttpResponse(invoker, httpRequest, INTERNAL_SERVER_ERROR, throwable, keepAlive),
						ctx.voidPromise());
			} else {
				ctx.write(new RestHttpResponse(invoker, httpRequest, INTERNAL_SERVER_ERROR, UNKNOWN, keepAlive),
						ctx.voidPromise());
			}
		});
	}

	private boolean doRequestFilter(FullHttpRequest request, Invoker<CompletableFuture<?>> invoker) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return true;
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

		for (int i = 0; i < filterLength; i++) {
			RestServerFilter filter = filters.get(i);
			if (!filter.onRecive(request)) {
				return false;
			}
		}

		return true;
	}

	private String toString(FullHttpRequest httpRequest) {
		String uri = httpRequest.uri();
		HttpMethod httpMethod = httpRequest.method();

		return httpMethod.name() + " " + uri;
	}

}
