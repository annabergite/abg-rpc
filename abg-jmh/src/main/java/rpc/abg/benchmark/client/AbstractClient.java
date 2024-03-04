package rpc.abg.benchmark.client;

import java.util.concurrent.atomic.AtomicInteger;

import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.benchmark.service.UserServiceServerImpl;

public abstract class AbstractClient {
	private final AtomicInteger counter = new AtomicInteger(0);
	private final UserService _serviceUserService = new UserServiceServerImpl();

	protected abstract UserService getUserService();

	public Object existUser() throws Exception {
		String email = String.valueOf(counter.getAndIncrement());
		return getUserService().existUser(email).join();
	}

	public Object createUser() throws Exception {
		int id = counter.getAndIncrement();
		User user = _serviceUserService.getUser(id).join();
		return getUserService().createUser(user).join();
	}

	public Object getUser() throws Exception {
		int id = counter.getAndIncrement();
		return getUserService().getUser(id).join();
	}

	public Object listUser() throws Exception {
		int pageNo = counter.getAndIncrement();
		return getUserService().listUser(pageNo).join();
	}

}
