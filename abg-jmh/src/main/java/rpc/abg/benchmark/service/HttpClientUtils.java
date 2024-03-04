package rpc.abg.benchmark.service;

import org.apache.http.HttpRequest;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.IgnoreSpecProvider;
import org.apache.http.protocol.HttpContext;

public class HttpClientUtils {

	public static final int SOCKET_TIMEOUT = 1500;
	public static final int CONNECTION_REQUEST_TIMEOUT = 3000;
	public static final int CONNECT_TIMEOUT = 3000;

	public static CloseableHttpClient createHttpClient(int concurrency) {
		HttpClientBuilder builder = HttpClientBuilder.create();

		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setDefaultMaxPerRoute(concurrency);
		connManager.setMaxTotal(concurrency);

		RequestConfig requestConfig = RequestConfig.custom()//
				.setAuthenticationEnabled(true)//
				.setSocketTimeout(SOCKET_TIMEOUT)//
				.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)//
				.setConnectTimeout(CONNECT_TIMEOUT)//
				.setRedirectsEnabled(true)//
				.setRelativeRedirectsAllowed(true)//
				.setMaxRedirects(15)//
				.build();

		SocketConfig socketConfig = SocketConfig.custom()//
				.setSoKeepAlive(true)//
				.setSoReuseAddress(true)//
				.build();

		CookieSpecProvider cookieSpecProvider = new IgnoreSpecProvider();
		Lookup<CookieSpecProvider> cookieSpecRegistry = RegistryBuilder.<CookieSpecProvider>create()//
				.register(CookieSpecs.DEFAULT, cookieSpecProvider)//
				.register(CookieSpecs.STANDARD, cookieSpecProvider)//
				.register(CookieSpecs.STANDARD_STRICT, cookieSpecProvider)//
				.build();

		builder.setConnectionManager(connManager);
		builder.setDefaultSocketConfig(socketConfig);
		builder.setDefaultRequestConfig(requestConfig);
		builder.setDefaultCookieSpecRegistry(cookieSpecRegistry);

		return builder.addInterceptorLast((HttpRequest request, HttpContext context) -> {
			request.removeHeaders("Host");
			request.removeHeaders("Accept-Encoding");
			//request.removeHeaders("Connection");
			request.removeHeaders("User-Agent");
		}).build();
	}
}
