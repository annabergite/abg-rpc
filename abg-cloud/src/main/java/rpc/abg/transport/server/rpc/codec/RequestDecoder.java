package rpc.abg.transport.server.rpc.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import rpc.abg.config.abgConstants;
import rpc.abg.serialization.Serializer;

public class RequestDecoder extends LengthFieldBasedFrameDecoder {
	private static final Log logger = LogFactory.getLog(RequestDecoder.class);

	private final Serializer serializer;

	public RequestDecoder(int maxFrameLength, Serializer serializer) {
		super(maxFrameLength, 0, abgConstants.HEADER_FIELD_LENGTH, 0, abgConstants.HEADER_FIELD_LENGTH);
		this.serializer = serializer;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf buffer = (ByteBuf) super.decode(ctx, in);

		if (buffer != null) {
			try {
				return serializer.readRequest(buffer);
			} finally {
				buffer.release();
			}
		}

		return null;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		if (logger.isErrorEnabled()) {
			logger.error("Exception caught on " + ctx.channel(), cause);
		}

		ctx.channel().close();
	}
}
