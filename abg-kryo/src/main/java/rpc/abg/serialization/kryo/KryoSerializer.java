package rpc.abg.serialization.kryo;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.netty.buffer.ByteBuf;
import rpc.abg.config.abgConstants;
import rpc.abg.param.MethodParam;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.protocol.ResponseStatus;
import rpc.abg.protocol.recycle.RecycleRequest;
import rpc.abg.protocol.recycle.RecycleResponse;
import rpc.abg.recycle.RecycleUtils;
import rpc.abg.serialization.Serializer;
import rpc.abg.serialization.TracerSerializer;
import rpc.abg.trace.Tracer;
import rpc.abg.util.ByteBufUtils;

public class KryoSerializer extends Serializer {
	private static final Log logger = LogFactory.getLog(KryoSerializer.class);

	private Map<Class<?>, Integer> registerMap;

	private final Supplier<KryoContext> producer = () -> {
		KryoContext kryoContext = new KryoContext();
		return kryoContext;
	};

	private final ThreadLocal<KryoContext> kryoContextHolder = ThreadLocal.withInitial(producer);
	private final TracerSerializer tracerSerializer = new TracerSerializer();

	@Override
	public boolean isSupportedClassId() {
		return true;
	}

	@Override
	public void setClassIds(Map<Class<?>, Integer> classIds) {
		this.registerMap = classIds;
	}

	private KryoContext kryoContext() {
		KryoContext kryoContext = kryoContextHolder.get();
		kryoContext.registerClassIds(registerMap);
		return kryoContext;
	}

	public void writeRequest(ByteBuf byteBuf, Request request) throws IOException {
		final int beginWriterIndex = byteBuf.writerIndex();

		byteBuf.writeInt(0);
		// byteBuf.writerIndex(beginWriterIndex + abgConstants.HEADER_FIELD_LENGTH);
		byteBuf.writeInt(request.getRequestId());
		ByteBufUtils.writeVarInt(byteBuf, request.getServiceId());
		tracerSerializer.write(byteBuf, request.getTracer());

		if (request.getMethodParam() == null) {
			byteBuf.writeBoolean(false);
		} else {
			byteBuf.writeBoolean(true);
			kryoContext().writeObject(byteBuf, request.getMethodParam());
		}

		int finishWriterIndex = byteBuf.writerIndex();
		int length = finishWriterIndex - beginWriterIndex - abgConstants.HEADER_FIELD_LENGTH;

		byteBuf.setInt(beginWriterIndex, length);

		// System.out.println("request content: " + new
		// String(ByteBufUtil.getBytes(byteBuf.duplicate())));
		// System.out.println("request length: " + byteBuf.writerIndex());

		RecycleUtils.release(request);
	}

	public Request readRequest(ByteBuf byteBuf) throws IOException {
		int requestId = byteBuf.readInt();
		int serviceId = ByteBufUtils.readVarInt(byteBuf);
		Tracer tracer = tracerSerializer.read(byteBuf);

		MethodParam methodParam = null;
		if (byteBuf.readBoolean()) {
			Class<?> clazz = getMethodParamClass(serviceId);
			methodParam = (MethodParam) kryoContext().readObject(byteBuf, clazz);
		}

		Request request = RecycleRequest.newInstance(requestId, serviceId, tracer, methodParam);

		return request;
	}

	public void writeResponse(ByteBuf byteBuf, Response response) throws IOException {
		final int beginWriterIndex = byteBuf.writerIndex();

		byteBuf.writeInt(0);
		// byteBuf.writerIndex(beginWriterIndex + abgConstants.HEADER_FIELD_LENGTH);
		byteBuf.writeInt(response.getRequestId());

		final int statusWriterIndex = byteBuf.writerIndex();

		try {
			byteBuf.writeByte(response.getStatusCode());
			tracerSerializer.write(byteBuf, response.getTracer());
			kryoContext().writeClassAndObject(byteBuf, response.getResult());
		} catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("kryo writeResponse error", e);
			}

			byteBuf.writerIndex(statusWriterIndex);
			byteBuf.writeByte(ResponseStatus.BAD_RESPONSE);
			tracerSerializer.write(byteBuf, response.getTracer());
			kryoContext().writeClassAndObject(byteBuf, e.getMessage());
		}

		int finishWriterIndex = byteBuf.writerIndex();
		int length = finishWriterIndex - beginWriterIndex - abgConstants.HEADER_FIELD_LENGTH;

		byteBuf.setInt(beginWriterIndex, length);

		RecycleUtils.release(response);
	}

	public Response readResponse(ByteBuf byteBuf) throws IOException {

		// System.out.println("response content: " + new
		// String(ByteBufUtil.getBytes(byteBuf.duplicate())));
		// System.out.println("response length: " + byteBuf.readableBytes());

		int requestId = byteBuf.readInt();
		byte statusCode = byteBuf.readByte();
		Tracer tracer = tracerSerializer.read(byteBuf);

		Object result = kryoContext().readClassAndObject(byteBuf);

		Response response = RecycleResponse.newInstance(requestId, statusCode, tracer, result);

		return response;
	}

}
