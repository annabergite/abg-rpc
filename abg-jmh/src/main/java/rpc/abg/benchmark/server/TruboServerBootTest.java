package rpc.abg.benchmark.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rpc.abg.benchmark.service.UserService;
import rpc.abg.boot.EnableabgServer;

@SpringBootApplication(scanBasePackages = { "rpc.abg.benchmark.service", "rpc.abg.benchmark.server" })
@EnableabgServer
public class TruboServerBootTest {

	@Autowired
	UserService userService;

	public static void main(String[] args) {
		SpringApplication.run(TruboServerBootTest.class, args);
	}
}
