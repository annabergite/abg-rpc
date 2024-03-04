package rpc.abg.common;

import java.lang.reflect.Method;

import rpc.abg.config.HostPort;
import rpc.abg.util.concurrent.AttachmentThreadUtils;

/**
 * 远程调用上下文
 * 
 * @author Annabergite
 *
 */
public final class RemoteContext {

	private static final int CLIENT_ADRESS_ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final int SERVER_ADRESS_ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final int INVOKER_ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();
	private static final int SERVICE_METHOD_NAME_ATTACHMENT_INDEX = AttachmentThreadUtils.nextVarIndex();

	/**
	 * 获取当前远程调用的客户端地址
	 * 
	 * @return
	 */
	public static HostPort getClientAddress() {
		return AttachmentThreadUtils.get(CLIENT_ADRESS_ATTACHMENT_INDEX);
	}

	public static void setClientAddress(HostPort clientAddress) {
		AttachmentThreadUtils.put(CLIENT_ADRESS_ATTACHMENT_INDEX, clientAddress);
	}

	/**
	 * 获取当前远程调用的服务端地址
	 * 
	 * @return
	 */
	public static HostPort getServerAddress() {
		return AttachmentThreadUtils.get(SERVER_ADRESS_ATTACHMENT_INDEX);
	}

	public static void setServerAddress(HostPort serverAddress) {
		AttachmentThreadUtils.put(SERVER_ADRESS_ATTACHMENT_INDEX, serverAddress);
	}

	/**
	 * 获取当前远程调用的接口方法, 可能为空
	 * 
	 * @return
	 */
	public static Method getRemoteMethod() {
		return AttachmentThreadUtils.get(INVOKER_ATTACHMENT_INDEX);
	}

	public static void setRemoteMethod(Method remoteMethod) {
		AttachmentThreadUtils.put(INVOKER_ATTACHMENT_INDEX, remoteMethod);
	}

	/**
	 * 获取当前远程调用的服务名称, 可能为空
	 * 
	 * @return
	 */
	public static String getServiceMethodName() {
		return AttachmentThreadUtils.get(SERVICE_METHOD_NAME_ATTACHMENT_INDEX);
	}

	public static void setServiceMethodName(String serviceMethodName) {
		AttachmentThreadUtils.put(SERVICE_METHOD_NAME_ATTACHMENT_INDEX, serviceMethodName);
	}

}
