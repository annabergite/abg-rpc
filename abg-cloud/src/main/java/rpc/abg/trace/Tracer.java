package rpc.abg.trace;

import java.io.Serializable;

import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.serialization.TracerSerializer;
import rpc.abg.util.uuid.ObjectId;

/**
 * 调用跟踪
 * 
 * @author Annabergite
 * 
 * @see Request
 * @see Response
 * @see TracerSerializer
 */
public class Tracer implements Serializable {

	private static final long serialVersionUID = 7844937939139983900L;

	// 就是一个全局的跟踪ID, 是跟踪的入口点, 根据需求来决定在哪生成traceId。比如一个http请求, 首先入口是web应用,
	// 一般看完整的调用链这里自然是traceId生成的起点, 结束点在web请求返回点。(traceId, spanId)确定一个Span
	private ObjectId traceId;

	// 这是下一层的请求跟踪ID, 这个也根据自己的需求, 比如认为一次rpc,
	// 一次sql执行等都可以是一个span。一个traceId包含一个以上的spanId。
	private long spanId;

	// 上一次请求跟踪ID, 用来将前后的请求串联起来。
	private long parentId;

	// Client Send, 表示客户端发起请求
	private long cs;

	// Client Received, 表示客户端获取到服务端返回信息
	private long cr;

	// Server Send, 表示服务端完成处理，并将结果发送给客户端
	private long ss;

	// Server Receive, 表示服务端收到请求
	private long sr;

	public ObjectId getTraceId() {
		return traceId;
	}

	public void setTraceId(ObjectId traceId) {
		this.traceId = traceId;
	}

	public long getParentId() {
		return parentId;
	}

	public void setParentId(long parentId) {
		this.parentId = parentId;
	}

	public long getSpanId() {
		return spanId;
	}

	public void setSpanId(long spanId) {
		this.spanId = spanId;
	}

	public long getCs() {
		return cs;
	}

	public void setCs(long cs) {
		this.cs = cs;
	}

	public long getCr() {
		return cr;
	}

	public void setCr(long cr) {
		this.cr = cr;
	}

	public long getSs() {
		return ss;
	}

	public void setSs(long ss) {
		this.ss = ss;
	}

	public long getSr() {
		return sr;
	}

	public void setSr(long sr) {
		this.sr = sr;
	}

	@Override
	public String toString() {
		return "Tracer{" + //
				"traceId=" + traceId + //
				", spanId=" + spanId + //
				", parentId=" + parentId + //
				", cs=" + cs + //
				", cr=" + cr + //
				", ss=" + ss + //
				", sr=" + sr + //
				'}';
	}
}
