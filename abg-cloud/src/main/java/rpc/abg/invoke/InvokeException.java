package rpc.abg.invoke;

public class InvokeException extends RuntimeException {

	private static final long serialVersionUID = 8836466407772412134L;

	public InvokeException() {
		super();
	}

	public InvokeException(String message) {
		super(message);
	}

	public InvokeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvokeException(String message, boolean writableStackTrace) {
		super(message, null, false, writableStackTrace);
	}

	public InvokeException(Throwable cause) {
		super(cause);
	}
}
