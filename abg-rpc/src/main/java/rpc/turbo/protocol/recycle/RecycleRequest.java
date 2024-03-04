package rpc.abg.protocol.recycle;

import io.netty.util.Recycler;
import io.netty.util.Recycler.Handle;
import rpc.abg.param.MethodParam;
import rpc.abg.protocol.Request;
import rpc.abg.recycle.Recycleable;
import rpc.abg.trace.Tracer;

public class RecycleRequest extends Request implements Recycleable {

	private static final long serialVersionUID = -3074626856820884094L;

	private static final Recycler<RecycleRequest> RECYCLER = new Recycler<RecycleRequest>() {
		protected RecycleRequest newObject(Recycler.Handle<RecycleRequest> handle) {
			return new RecycleRequest(handle);
		}
	};

	public static RecycleRequest newInstance() {
		return RECYCLER.get();
	}

	public static RecycleRequest newInstance(int requestId, int serviceId, Tracer tracer, MethodParam methodParam) {
		RecycleRequest recycleRequest = RECYCLER.get();

		recycleRequest.setRequestId(requestId);
		recycleRequest.setServiceId(serviceId);
		recycleRequest.setTracer(tracer);
		recycleRequest.setMethodParam(methodParam);

		return recycleRequest;
	}

	private final transient Recycler.Handle<RecycleRequest> handle;

	private RecycleRequest(Handle<RecycleRequest> handle) {
		this.handle = handle;
	}

	@Override
	public void recycle() {
		setMethodParam(null);// 加快垃圾回收
		setTracer(null);// 加快垃圾回收

		handle.recycle(this);
	}
}
