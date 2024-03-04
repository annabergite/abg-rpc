package rpc.abg.benchmark.service;

import org.springframework.stereotype.Component;
import rpc.abg.annotation.abgFailover;
import rpc.abg.benchmark.bean.User;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Component("userService")
@abgFailover(service = UserService.class)
public class UserServiceFailover {

	public CompletableFuture<Boolean> createUser(User user) {
		return CompletableFuture.completedFuture(Boolean.FALSE);
	}

	public static void main(String[] args) {
		System.out.println(Arrays.toString(UserServiceServerImpl.class.getDeclaredClasses()));
	}

}
