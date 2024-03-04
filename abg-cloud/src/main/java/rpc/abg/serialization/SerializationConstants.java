package rpc.abg.serialization;

public final class SerializationConstants {

	/** 是否开启快速String模式，开启后将不兼容通常实现 */
	public static final boolean INCOMPATIBLE_FAST_STRING_FORMAT = true;

	public static final byte STRING_NULL = 0;

	public static final byte STRING_EMPTY = 1;

	public static final byte STRING_LATIN1 = 2;

	public static final byte STRING_UTF8 = 3;
}
