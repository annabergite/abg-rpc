package rpc.abg.benchmark.server;

import org.springframework.stereotype.Component;

import rpc.abg.boot.abgServerAware;
import rpc.abg.common.RemoteContext;
import rpc.abg.filter.RpcServerFilter;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.server.abgServer;
import rpc.abg.trace.TracerContext;
import rpc.abg.trace.Tracer;

//@Component
public class TruboServerFilterTest implements abgServerAware {

	@Override
	public void setabgServer(abgServer abgServer) {
		abgServer.addFirst(new RpcServerFilter() {

			@Override
			public void onSend(Request request, Response response) {
				try {
					Tracer tracer = request.getTracer();

					if (tracer != null) {
						RemoteContext.getClientAddress().toString();
						RemoteContext.getServerAddress().toString();
						RemoteContext.getServiceMethodName();

						TracerContext.setTracer(tracer);
						response.setTracer(tracer);
					}

					System.out.println(response);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean onRecive(Request request) {
				System.out.println(request);
				return true;
			}

			@Override
			public void onError(Request request, Response response, Throwable throwable) {
				System.out.println(throwable);
			}
		});

	}

}
