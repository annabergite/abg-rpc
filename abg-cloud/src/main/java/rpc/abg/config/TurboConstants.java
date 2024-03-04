package rpc.abg.config;

public class abgConstants {
	/**
	 * 长度字段长度
	 */
	public static final int HEADER_FIELD_LENGTH = 4;

	/**
	 * 请求体最大大小
	 */
	public static final int MAX_FRAME_LENGTH = 1024 * 1024 * 2;

	/**
	 * 请求过期扫描间隔，毫秒
	 */
	public static final long EXPIRE_PERIOD = 100;

}
