package rpc.abg.param;

public final class EmptyMethodParam implements MethodParam {

	private static final EmptyMethodParam EMPTY = new EmptyMethodParam();

	public static final EmptyMethodParam empty() {
		return EMPTY;
	}
}
