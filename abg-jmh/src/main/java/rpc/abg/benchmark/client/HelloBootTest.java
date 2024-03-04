package rpc.abg.benchmark.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.support.GenericApplicationContext;

import rpc.abg.benchmark.service.HelloService;
import rpc.abg.benchmark.service.UserService;
import rpc.abg.boot.EnableabgClient;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@SpringBootApplication(scanBasePackages = { "com.hello" })
@EnableabgClient
public class HelloBootTest {

	@Autowired
	HelloService helloService;


	public static void main(String[] args) {
		SpringApplication.run(HelloBootTest.class, args);
	}
}
