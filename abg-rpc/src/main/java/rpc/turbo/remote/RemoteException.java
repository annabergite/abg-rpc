package rpc.abg.remote;

public class RemoteException extends RuntimeException {
	private static final long serialVersionUID = 2308364793082349611L;

	public RemoteException() {
		super();
	}

	public RemoteException(String message) {
		super(message);
	}

	public RemoteException(String message, boolean writableStackTrace) {
		super(message, null, false, writableStackTrace);
	}

	public RemoteException(String message, Throwable cause) {
		super(message, cause);
	}

	public RemoteException(Throwable cause) {
		super(cause);
	}
}
