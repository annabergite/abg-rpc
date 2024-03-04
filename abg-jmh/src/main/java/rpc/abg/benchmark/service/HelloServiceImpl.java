package rpc.abg.benchmark.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

@Component
public class HelloServiceImpl implements HelloService {
	@Override
	public CompletableFuture<String> hello(String msg) {
		return CompletableFuture.completedFuture(msg);
	}
}
