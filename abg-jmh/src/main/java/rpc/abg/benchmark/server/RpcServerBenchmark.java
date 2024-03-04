package rpc.abg.benchmark.server;

import java.util.Map;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.config.HostPort;
import rpc.abg.server.abgServer;

public class RpcServerBenchmark {
	public static void main(String[] args) throws Exception {
		ResourceLeakDetector.setLevel(Level.DISABLED);
		// CtClass.debugDump = "d:/debugDump";

		try (abgServer server = new abgServer("shop", "auth");) {
			Map<Class<?>, Object> services = Map.of(UserService.class, new UserServiceServerImpl());
			server.registerService(services);

			/*
			 * server.addFirst(new RpcServerFilter() {
			 * 
			 * @Override public void onSend(Request request, Response response) { try {
			 * Tracer tracer = request.getTracer();
			 * 
			 * if (tracer != null) { response.setTracer(tracer); }
			 * 
			 * } catch (Exception e) { e.printStackTrace(); } }
			 * 
			 * @Override public boolean onRecive(Request request) { try { Tracer tracer =
			 * request.getTracer();
			 * 
			 * if (tracer != null) { RemoteContext.getClientAddress().toString();
			 * RemoteContext.getServerAddress().toString();
			 * RemoteContext.getServiceMethodName();
			 * 
			 * TracerContext.setTracer(tracer); } } catch (Exception e) {
			 * e.printStackTrace(); }
			 * 
			 * return true; }
			 * 
			 * @Override public void onError(Request request, Response response, Throwable
			 * throwable) { } });
			 */

			server.startRpcServer(new HostPort("127.0.0.1", 8080));
			server.waitUntilShutdown();
		}
	}
}
