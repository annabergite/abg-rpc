package rpc.abg.benchmark.client;

import org.springframework.stereotype.Component;

import rpc.abg.boot.abgClientAware;
import rpc.abg.client.abgClient;
import rpc.abg.common.RemoteContext;
import rpc.abg.filter.RpcClientFilter;
import rpc.abg.protocol.Request;
import rpc.abg.protocol.Response;
import rpc.abg.trace.TracerContext;
import rpc.abg.trace.Tracer;

@Component
public class TruboClientFilterTest implements abgClientAware {

	@Override
	public void setabgClient(abgClient abgClient) {
		abgClient.addFirst(new RpcClientFilter() {

			@Override
			public boolean onSend(Request request) {
				try {
					Tracer tracer = TracerContext.nextTracer();

					if (tracer != null) {
						RemoteContext.getClientAddress().toString();
						RemoteContext.getServerAddress().toString();
						RemoteContext.getServiceMethodName();

						request.setTracer(tracer);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				System.out.println(request);

				return true;
			}

			@Override
			public void onRecive(Request request, Response response) {
				System.out.println(response);
			}

			@Override
			public void onError(Request request, Response response, Throwable throwable) {
				System.out.println(throwable);
			}
		});

	}

}
