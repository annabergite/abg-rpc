package rpc.abg.config;

public class ConfigException extends RuntimeException {

	private static final long serialVersionUID = -7816485423426653098L;

	public ConfigException() {
		super();
	}

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(String message, boolean writableStackTrace) {
		super(message, null, false, writableStackTrace);
	}

	public ConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigException(Throwable cause) {
		super(cause);
	}
}
