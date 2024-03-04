package rpc.abg.protocol;

/**
 * 响应码
 * 
 * @author Annabergite
 *
 */
public interface ResponseStatus {
	public static final byte OK = 1;
	public static final byte NOT_FOUND = 2;
	public static final byte SERVER_ERROR = 3;
	public static final byte BAD_REQUEST = 4;
	public static final byte BAD_RESPONSE = 5;
	public static final byte TIMEOUT = 6;
	public static final byte CLIENT_FILTER_DENY = 7;
	public static final byte SERVER_FILTER_DENY = 8;
}
