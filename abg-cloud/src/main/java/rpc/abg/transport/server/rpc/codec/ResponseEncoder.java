package rpc.abg.transport.server.rpc.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import rpc.abg.protocol.Response;
import rpc.abg.serialization.Serializer;

public class ResponseEncoder extends MessageToByteEncoder<Response> {
	private static final Log logger = LogFactory.getLog(ResponseEncoder.class);

	private final Serializer serializer;

	public ResponseEncoder(Serializer serializer) {
		this.serializer = serializer;
	}

	protected void encode(ChannelHandlerContext ctx, Response response, ByteBuf buffer) throws Exception {
		serializer.writeResponse(buffer, response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.channel().close();
	}

}
