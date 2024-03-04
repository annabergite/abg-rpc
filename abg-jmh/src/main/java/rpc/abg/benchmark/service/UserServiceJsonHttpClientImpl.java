package rpc.abg.benchmark.service;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import rpc.abg.benchmark.bean.Page;
import rpc.abg.benchmark.bean.User;

/**
 * only for client
 * 
 * @author Annabergite
 *
 */
public class UserServiceJsonHttpClientImpl implements UserService, Closeable {

	private static final String URL_EXIST_USER = "http://127.0.0.1:8080/shop/auth/user/exist?v=2&email=";
	private static final String URL_CREATE_USER = "http://127.0.0.1:8080/shop/auth/user/create?v=2";
	private static final String URL_GET_USER = "http://127.0.0.1:8080/shop/auth/user/get?v=2&id=";
	private static final String URL_LIST_USER = "http://127.0.0.1:8080/shop/auth/user/list?v=1&pageNo=";

	private final CloseableHttpClient client;
	private final ObjectMapper objectMapper = JsonUtils.objectMapper;
	private final JavaType userPageType = objectMapper.getTypeFactory()//
			.constructParametricType(Page.class, User.class);

	public UserServiceJsonHttpClientImpl(int concurrency) {
		client = HttpClientUtils.createHttpClient(concurrency);
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	@Override
	public CompletableFuture<Boolean> existUser(String email) {
		try {
			String url = URL_EXIST_USER + email;

			HttpGet request = new HttpGet(url);
			CloseableHttpResponse response = client.execute(request);

			String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

			return CompletableFuture.completedFuture("true".equals(result));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<Boolean> createUser(User user) {
		try {
			byte[] bytes = objectMapper.writeValueAsBytes(user);

			HttpPost request = new HttpPost(URL_CREATE_USER);
			HttpEntity entity = EntityBuilder.create().setBinary(bytes).build();
			request.setEntity(entity);

			CloseableHttpResponse response = client.execute(request);

			String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

			return CompletableFuture.completedFuture("true".equals(result));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<User> getUser(long id) {
		try {
			String url = URL_GET_USER + id;

			HttpGet request = new HttpGet(url);
			CloseableHttpResponse response = client.execute(request);

			byte[] bytes = EntityUtils.toByteArray(response.getEntity());

			return CompletableFuture.completedFuture(objectMapper.readValue(bytes, User.class));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public CompletableFuture<Page<User>> listUser(int pageNo) {
		try {
			String url = URL_LIST_USER + pageNo;

			HttpGet request = new HttpGet(url);
			CloseableHttpResponse response = client.execute(request);

			byte[] bytes = EntityUtils.toByteArray(response.getEntity());

			return CompletableFuture.completedFuture(objectMapper.readValue(bytes, userPageType));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		try (UserServiceJsonHttpClientImpl userService = new UserServiceJsonHttpClientImpl(256)) {
			System.out.println(userService.existUser("1236").join());
			System.out.println(userService.getUser(123).join());
			System.out.println(userService.listUser(123).join());
		}
	}

}
