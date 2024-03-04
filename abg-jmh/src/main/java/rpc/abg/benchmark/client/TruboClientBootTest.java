package rpc.abg.benchmark.client;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;

import rpc.abg.benchmark.bean.User;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.boot.EnableabgClient;

@SpringBootApplication(scanBasePackages = { "rpc.abg.benchmark.service", "rpc.abg.benchmark.client" })
@EnableabgClient
public class TruboClientBootTest {

	@Autowired
	GenericApplicationContext applicationContext;

	@Autowired
	UserService userService;

	@Autowired
	// @Qualifier("userService")
	UserService userService2;

	@PostConstruct
	public void test() {
		System.out.println(Arrays.toString(applicationContext.getBeanNamesForType(UserService.class)));
		System.out.println("userService: " + userService.getClass().getName());
		System.out.println("userService2: " + userService2.getClass().getName());

		System.out.println("existUser:");
		boolean existUser = userService.existUser("ziboqizhong@outlook.com").join();
		System.out.println(existUser);
		System.out.println("=====================");
		System.out.println();

		System.out.println("getUser:");
		User user = userService.getUser(1).join();
		System.out.println(user);
		System.out.println("=====================");
		System.out.println();

		System.out.println("createUser:");
		System.out.println(userService2.createUser(user).join());
		System.out.println("=====================");
		System.out.println();

		System.out.println("listUser:");
		System.out.println(userService.listUser(1).join());
		System.out.println("=====================");
		System.out.println();
	}

	public static void main(String[] args) {
		SpringApplication.run(TruboClientBootTest.class, args);
	}
}
