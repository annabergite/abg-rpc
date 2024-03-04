package rpc.abg.transport.client.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import rpc.abg.config.abgConstants;
import rpc.abg.protocol.Response;
import rpc.abg.serialization.Serializer;
import rpc.abg.transport.client.future.FutureContainer;

public class ResponseDecoder extends LengthFieldBasedFrameDecoder {
	private static final Log logger = LogFactory.getLog(ResponseDecoder.class);

	private final Serializer serializer;
	private final FutureContainer futureContainer;

	public ResponseDecoder(int maxFrameLength, Serializer serializer, FutureContainer futureContainer) {
		super(maxFrameLength, 0, abgConstants.HEADER_FIELD_LENGTH, 0, abgConstants.HEADER_FIELD_LENGTH);
		this.serializer = serializer;
		this.futureContainer = futureContainer;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf buffer = (ByteBuf) super.decode(ctx, in);

		if (buffer != null) {
			try {
				Response response = serializer.readResponse(buffer);
				futureContainer.notifyResponse(response);
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

		this.futureContainer.close();
	}

}
