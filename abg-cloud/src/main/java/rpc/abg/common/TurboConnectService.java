package rpc.abg.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import rpc.abg.annotation.AbgService;

/**
 * 基本的内置服务，建立连接后需要调用
 * 
 * @author Annabergite
 *
 */
@AbgService(version = "1.0.0", rest = "/abg-service")
public interface abgConnectService {

	public static final int SERVICE_HEARTBEAT = 0;
	public static final int SERVICE_CLASS_REGISTER = 1;
	public static final int SERVICE_METHOD_REGISTER = 2;
	public static final int SERVICE_REST_REGISTER = 3;
	public static final int SERVICE_CLASS_ID_REGISTER = 4;

	/**
	 * 固定死顺序，保证serviceId为预设值
	 */
	public static final Map<String, Integer> serviceOrderMap = Map.of(//
			"heartbeat", SERVICE_HEARTBEAT, //
			"getClassRegisterList", SERVICE_CLASS_REGISTER, //
			"getMethodRegisterMap", SERVICE_METHOD_REGISTER, //
			"getRestRegisterList", SERVICE_REST_REGISTER, //
			"getClassIdMap", SERVICE_CLASS_ID_REGISTER);

	/**
	 * 心跳，true is ok
	 * 
	 * @return
	 */
	@AbgService(version = "1.0.0", rest = "/heartbeat")
	default CompletableFuture<Boolean> heartbeat() {
		return CompletableFuture.completedFuture(Boolean.FALSE);
	}

	/**
	 * 获取已注册的RPC类，classString
	 * 
	 * @return
	 */
	@AbgService(version = "1.0.0", rest = "/class/list")
	CompletableFuture<List<String>> getClassRegisterList();

	/**
	 * 获取已注册的RPC方法，key:serviceId, value:methodString
	 * 
	 * @return
	 */
	@AbgService(version = "1.0.0", rest = "/method/list")
	CompletableFuture<Map<String, Integer>> getMethodRegisterMap();

	/**
	 * 获取已注册的rest服务列表
	 * 
	 * @return
	 */
	@AbgService(version = "1.0.0", rest = "/rest/list")
	CompletableFuture<List<String>> getRestRegisterList();

	/**
	 * 获取已注册的 classId
	 * 
	 * @return
	 */
	@AbgService(version = "1.0.0", rest = "/class-id/list")
	CompletableFuture<Map<String, Integer>> getClassIdMap();

}
