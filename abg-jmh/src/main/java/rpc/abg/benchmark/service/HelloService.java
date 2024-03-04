package rpc.abg.benchmark.service;

import java.util.concurrent.CompletableFuture;

import rpc.abg.annotation.abgService;

@abgService(version = "1.0.0")
public interface HelloService {

	@abgService(version = "1.0.0", rest = "hello")
	default CompletableFuture<String> hello(String msg) {
		// default实现会自动注册为失败回退方法，当远程调用失败时执行
		return CompletableFuture.completedFuture("error");
	}
}
