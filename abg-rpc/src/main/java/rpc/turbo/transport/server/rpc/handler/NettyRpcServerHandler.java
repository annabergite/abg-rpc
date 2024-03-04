package rpc.abg.transport.server.rpc.handler;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import rpc.abg.common.RemoteContext;
import rpc.abg.config.HostPort;
import rpc.abg.filter.RpcServerFilter;
import rpc.abg.invoke.InvokeException;
import rpc.abg.invoke.Invoker;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.protocol.ResponseStatus;
import rpc.abg.protocol.recycle.RecycleResponse;

public class NettyRpcServerHandler extends SimpleChannelInboundHandler<Request> {
	private static final Log logger = LogFactory.getLog(NettyRpcServerHandler.class);
	private static final Throwable UNKNOWN = new InvokeException("UNKNOWN ERROR", false);

	private final ServerInvokerFactory invokerFactory;
	private final CopyOnWriteArrayList<RpcServerFilter> filters;
	private HostPort clientAddress;
	private HostPort serverAddress;

	public NettyRpcServerHandler(ServerInvokerFactory invokerFactory, CopyOnWriteArrayList<RpcServerFilter> filters) {
		this.invokerFactory = invokerFactory;
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

	protected void channelRead0(ChannelHandlerContext ctx, final Request request) throws Exception {
		final int requestId = request.getRequestId();
		final Invoker<CompletableFuture<?>> invoker = invokerFactory.get(request.getServiceId());

		if (invoker == null) {
			if (logger.isErrorEnabled()) {
				logger.error("not support this serviceId: " + request.getServiceId());
			}

			ctx.channel().close();
			return;
		}

		boolean allowHandle = doRequestFilter(request, invoker);

		final RecycleResponse response = RecycleResponse.newInstance(request);

		if (!allowHandle) {
			response.setRequestId(requestId);
			response.setStatusCode(ResponseStatus.SERVER_FILTER_DENY);
			response.setTracer(null);
			response.setResult(RpcServerFilter.SERVER_FILTER_DENY);

			doResponseFilter(request, response, invoker, null);

			ctx.writeAndFlush(response, ctx.voidPromise());

			return;
		}

		CompletableFuture<?> future = invoker.invoke(request.getMethodParam());

		future.whenComplete((result, throwable) -> {
			response.setRequestId(requestId);
			response.setTracer(null);

			if (result != null) {
				response.setStatusCode(ResponseStatus.OK);
				response.setResult(result);

				doResponseFilter(request, response, invoker, null);
			} else if (throwable != null) {
				response.setStatusCode(ResponseStatus.SERVER_ERROR);
				response.setResult(Arrays.toString(throwable.getStackTrace()));

				doResponseFilter(request, response, invoker, throwable);
			} else {
				response.setStatusCode(ResponseStatus.SERVER_ERROR);
				response.setResult(UNKNOWN);

				doResponseFilter(request, response, invoker, UNKNOWN);
			}

			ctx.writeAndFlush(response, ctx.voidPromise());
		});
	}

	private boolean doRequestFilter(Request request, Invoker<CompletableFuture<?>> invoker) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return true;
		}

		RemoteContext.setServerAddress(serverAddress);
		RemoteContext.setClientAddress(clientAddress);
		RemoteContext.setRemoteMethod(invoker.getMethod());
		RemoteContext.setServiceMethodName(invokerFactory.getServiceMethodName(invoker.getServiceId()));

		for (int i = 0; i < filterLength; i++) {
			RpcServerFilter filter = filters.get(i);
			if (!filter.onRecive(request)) {
				return false;
			}
		}

		return true;
	}

	private void doResponseFilter(Request request, Response response, Invoker<CompletableFuture<?>> invoker,
			Throwable throwable) {
		final int filterLength = filters.size();
		if (filterLength == 0) {
			return;
		}

		RemoteContext.setServerAddress(serverAddress);
		RemoteContext.setClientAddress(clientAddress);
		RemoteContext.setRemoteMethod(invoker.getMethod());
		RemoteContext.setServiceMethodName(invokerFactory.getServiceMethodName(invoker.getServiceId()));

		if (response.getStatusCode() == ResponseStatus.OK) {
			for (int i = 0; i < filterLength; i++) {
				RpcServerFilter filter = filters.get(i);
				filter.onSend(request, response);
			}
		} else {
			for (int i = 0; i < filterLength; i++) {
				RpcServerFilter filter = filters.get(i);
				filter.onError(request, response, throwable);
			}
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.channel().close();
	}
}
