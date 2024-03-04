package rpc.abg.protocol;

import rpc.abg.serialization.Serializer;
import rpc.abg.trace.Tracer;

import java.io.Serializable;

/**
 * 响应体
 * 
 * @author Annabergite
 * 
 * @see Serializer
 *
 */
public class Response implements Serializable {
	private static final long serialVersionUID = -2827803061483152127L;

	private int requestId;
	private byte statusCode;
	private Tracer tracer;
	private Object result;

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public byte getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(byte statusCode) {
		this.statusCode = statusCode;
	}

	public Tracer getTracer() {
		return tracer;
	}

	public void setTracer(Tracer tracer) {
		this.tracer = tracer;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "Response{" + //
				"requestId=" + requestId + //
				", statusCode=" + statusCode + //
				", tracer=" + tracer + //
				", result=" + result + //
				'}';
	}
}
