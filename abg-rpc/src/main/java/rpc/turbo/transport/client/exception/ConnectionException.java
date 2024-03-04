package rpc.abg.transport.client.exception;

public class ConnectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConnectionException() {
		super();
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(String message, boolean writableStackTrace) {
		super(message, null, false, writableStackTrace);
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionException(Throwable cause) {
		super(cause);
	}

}
