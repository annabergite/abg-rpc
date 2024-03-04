# abg-cloud

**abg-cloud 是一款速度超凡的异步响应式RPC框架**
## 功能特点
 - 仅支持异步调用, Service 接口所有 public 方法返回值都必须为 CompletableFuture
 - 配置定义在 Service 接口上, 而非实现类上, 方法实现者和调用者都不需要引入奇奇怪怪的注解
 - 支持 REST 调用
 - 支持失败回退, 支持熔断, 支持心跳, 支持自动重连
 - 支持自定义 服务注册 负载均衡 序列化
 - 支持 Filter, 可通过该机制实现 Tracing 限流限速 黑白名单 等功能
 - 支持 spring boot

## Quick Start

Maven依赖: 
```xml
<!-- 必须引入 -->
<dependency>
    <groupId>com.abg-cloud</groupId>
    <artifactId>abg-cloud</artifactId>
    <version>0.0.9</version>
</dependency>

<!-- 序列化方式，kryo protostuff 两个中任选一个 -->
<dependency>
    <groupId>com.abg-cloud</groupId>
    <artifactId>abg-kryo</artifactId>
    <version>0.0.9</version>
</dependency>

<!-- 序列化方式，kryo protostuff 两个中任选一个 -->
<dependency>
    <groupId>com.abg-cloud</groupId>
    <artifactId>abg-protostuff</artifactId>
    <version>0.0.9</version>
</dependency>

<!-- zk注册中心，可选引入 -->
<dependency>
    <groupId>com.abg-cloud</groupId>
    <artifactId>abg-register-zk</artifactId>
    <version>0.0.9</version>
</dependency>

<!-- SpringBoot集成，可选引入 -->
<dependency>
    <groupId>com.abg-cloud</groupId>
    <artifactId>abg-spring-boot-starter</artifactId>
    <version>0.0.9</version>
</dependency>
```

1.定义接口
```java
@abgService(version = "1.0.0")
public interface HelloService {

	@abgService(version = "1.0.0", rest = "hello")
	default CompletableFuture<String> hello(String msg) {
		// default实现会自动注册为失败回退方法，当远程调用失败时执行
		return CompletableFuture.completedFuture("error");
	}
}
```

2.服务端实现接口
```java
@Component
public class HelloServiceImpl implements HelloService {
	@Override
	public CompletableFuture<String> hello(String msg) {
		return CompletableFuture.completedFuture(msg);
	}
}
```

3.配置abg-server.conf, 声明 服务器地址 序列化协议 注册地址 等信息

4.服务端启动
```java
@SpringBootApplication(scanBasePackages = { "com.hello" })
@EnableabgServer
public class TruboServerBootTest {
	public static void main(String[] args) {
		SpringApplication.run(TruboServerBootTest.class, args);
	}
}
```
----------

5.客户端调用
```java
@Component
public class HelloReferTest {
	@Autowired
	HelloService helloService;

	public void doSomeThing(String msg) {
		helloService.hello(msg)
			.thenAccept(message -> System.out.println(message));
	}
}
```

6.配置abg-client.conf, 声明 服务器地址 序列化协议 注册地址 等信息

7.客户端启动
```java
@SpringBootApplication(scanBasePackages = { "com.hello" })
@EnableabgClient
public class HelloBootTest {
	public static void main(String[] args) {
		SpringApplication.run(HelloBootTest.class, args);
	}
}
```

## abg 技术原理
- [如何编写高性能的 RPC 框架](https://www.jianshu.com/p/7182b8751e75) 
- [RPC 异步响应式调用](https://www.jianshu.com/p/1e406ffa5f90)
