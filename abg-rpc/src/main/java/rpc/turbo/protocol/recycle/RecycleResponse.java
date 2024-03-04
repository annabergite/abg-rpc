package rpc.abg.protocol.recycle;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.recycle.Recycleable;
import rpc.abg.trace.Tracer;

public class RecycleResponse extends Response implements Recycleable {

	private static final long serialVersionUID = -3053063550774417994L;

	private static final Recycler<RecycleResponse> RECYCLER = new Recycler<RecycleResponse>() {
		protected RecycleResponse newObject(Recycler.Handle<RecycleResponse> handle) {
			return new RecycleResponse(handle);
		}
	};

	public static RecycleResponse newInstance() {
		return RECYCLER.get();
	}

	public static RecycleResponse newInstance(Request request) {
		RecycleResponse recycleResponse = RECYCLER.get();
		recycleResponse.setRequest(request);

		return recycleResponse;
	}

	public static RecycleResponse newInstance(int requestId, byte statusCode, Tracer tracer, Object result) {
		RecycleResponse recycleResponse = RECYCLER.get();

		recycleResponse.setRequestId(requestId);
		recycleResponse.setStatusCode(statusCode);
		recycleResponse.setTracer(tracer);
		recycleResponse.setResult(result);

		return recycleResponse;
	}

	private final transient Recycler.Handle<RecycleResponse> handle;
	private transient Request request;

	private RecycleResponse(Handle<RecycleResponse> handle) {
		this.handle = handle;
	}

	@Override
	public void recycle() {
		if (this.request instanceof Recycleable) {
			((Recycleable) request).recycle();
		}

		this.request = null;// 加快垃圾回收
		setResult(null);// 加快垃圾回收
		setTracer(null);// 加快垃圾回收

		handle.recycle(this);
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

}
