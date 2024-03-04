package rpc.abg.benchmark.server;

import java.util.Map;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;
import rpc.abg.config.HostPort;
import rpc.abg.server.abgServer;

public class RestServerBenchmark {
	public static void main(String[] args) throws Exception {
		ResourceLeakDetector.setLevel(Level.DISABLED);
		// CtClass.debugDump = "d:/debugDump";

		try (abgServer server = new abgServer("shop", "auth");) {
			Map<Class<?>, Object> services = Map.of(UserService.class, new UserServiceServerImpl());
			server.registerService(services);

			server.startRestServer(new HostPort("0.0.0.0", 8080));
			server.waitUntilShutdown();
		}
	}
}
