package rpc.abg.serialization;

import java.io.IOException;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import rpc.abg.param.MethodParam;
import rpc.abg.param.MethodParamClassResolver;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;

/**
 * 序列化，非通用，仅适用于Request、Response
 * 
 * @author Annabergite
 *
 */
public abstract class Serializer {

	private MethodParamClassResolver methodParamClassResolver;

	/**
	 * 仅server端设置
	 * 
	 * @param methodParamClassResolver
	 */
	public final void setClassResolver(MethodParamClassResolver methodParamClassResolver) {
		this.methodParamClassResolver = methodParamClassResolver;
	}

	/**
	 * server端根据serviceId获取相应的MethodParam class
	 * 
	 * @param serviceId
	 * @return 具体的MethodParam class
	 * 
	 * @see MethodParam
	 */
	public final Class<? extends MethodParam> getMethodParamClass(int serviceId) {
		return methodParamClassResolver.getMethodParamClass(serviceId);
	}

	/**
	 * 序列化request
	 * 
	 * @param byteBuf
	 * @param request
	 * @throws IOException
	 */
	public abstract void writeRequest(ByteBuf byteBuf, Request request) throws IOException;

	/**
	 * 反序列化request
	 * 
	 * @param byteBuf
	 * @return
	 * @throws IOException
	 */
	public abstract Request readRequest(ByteBuf byteBuf) throws IOException;

	/**
	 * 序列化response
	 * 
	 * @param byteBuf
	 * @param response
	 * @throws IOException
	 */
	public abstract void writeResponse(ByteBuf byteBuf, Response response) throws IOException;

	/**
	 * 反序列化response
	 * 
	 * @param byteBuf
	 * @return
	 * @throws IOException
	 */
	public abstract Response readResponse(ByteBuf byteBuf) throws IOException;

	/**
	 * 是否支持classID
	 * 
	 * @return
	 */
	public boolean isSupportedClassId() {
		return false;
	}

	/**
	 * 设置classID
	 * 
	 * @param classIds
	 */
	public void setClassIds(Map<Class<?>, Integer> classIds) {
		return;
	}
}
