package rpc.abg.transport.client.exception;

public class ResponseTimeoutException extends Exception {
	private static final long serialVersionUID = -1954200194184284003L;

	public static final ResponseTimeoutException NONE_STACK_TRACE = new ResponseTimeoutException("it's timeout", false);

	public ResponseTimeoutException() {
		super();
	}

	public ResponseTimeoutException(String message) {
		super(message);
	}

	public ResponseTimeoutException(String message, boolean writableStackTrace) {
		super(message, null, false, writableStackTrace);
	}

	public ResponseTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResponseTimeoutException(Throwable cause) {
		super(cause);
	}
}
