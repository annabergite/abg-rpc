package rpc.abg.boot;

import static rpc.abg.util.tuple.Tuple.tuple;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import rpc.abg.annotation.AbgService;
import rpc.abg.config.server.ServerConfig;
import rpc.abg.invoke.ServerInvokerFactory;
import rpc.abg.server.abgServer;
import rpc.abg.util.tuple.Tuple2;
import rpc.abg.util.tuple.Tuple3;

@Configuration
@ConditionalOnClass({ AbgService.class, EnableabgServer.class })
@Order(Ordered.LOWEST_PRECEDENCE)
public class abgServerStarter {
	private static final Log logger = LogFactory.getLog(abgServerStarter.class);

	@Autowired
	private GenericApplicationContext applicationContext;

	private abgServer abgServer;

	@PreDestroy
	public void close() {
		if (abgServer == null) {
			return;
		}

		try {
			abgServer.close();
		} catch (Throwable e) {
			if (logger.isErrorEnabled()) {
				logger.error("abgServer关闭失败!", e);
			}
		}
	}

	@PostConstruct
	public void startTuroboServer() {

		ServerConfig serverConfig;
		try {
			serverConfig = ServerConfig.parse("abg-server.conf");
		} catch (com.typesafe.config.ConfigException configException) {
			if (logger.isErrorEnabled()) {
				logger.error("abg-server.conf 格式错误，无法开启abgServer!", configException);
			}

			return;
		} catch (Throwable e) {
			if (logger.isErrorEnabled()) {
				logger.error("类路径中找不到 abg-server.conf，无法开启abgServer!", e);
			}

			return;
		}

		@SuppressWarnings("rawtypes")
		Collection<Tuple3<AbgService, Class, Object>> AbgServiceList = getAbgServiceList();

		if (AbgServiceList.isEmpty()) {
			if (logger.isErrorEnabled()) {
				logger.error("找不到有效的 AbgService，无法开启abgServer!");
			}

			return;
		}

		ServerInvokerFactory invokerFactory = new ServerInvokerFactory(serverConfig.getGroup(), serverConfig.getApp());
		AbgServiceList.forEach(t3 -> {
			invokerFactory.register(t3._2, t3._3);
		});

		try {
			abgServer = new abgServer(serverConfig, invokerFactory);
			abgServer.startAndRegisterServer();

			Map<String, abgServerAware> abgServerAwareMap//
					= applicationContext.getBeansOfType(abgServerAware.class);

			abgServerAwareMap.forEach((key, value) -> value.setabgServer(abgServer));
		} catch (Throwable e) {
			if (logger.isErrorEnabled()) {
				logger.error("abgServer启动失败!", e);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private Collection<Tuple3<AbgService, Class, Object>> getAbgServiceList() {
		Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);

		if (beans == null || beans.isEmpty()) {
			return List.of();
		}

		Map<Class, Tuple3<AbgService, Class, Object>> AbgServiceMap = beans//
				.entrySet()//
				.parallelStream()//
				.map(kv -> {
					Object bean = kv.getValue();
					Tuple2<AbgService, Class> t2 = getAbgService(bean);

					if (t2 == null) {
						return null;
					}

					if (logger.isDebugEnabled()) {
						logger.debug("find AbgService: " + kv.getKey() + " " + t2._2.getName() + t2._1);
					}

					return tuple(t2._1, t2._2, bean);
				})//
				.filter(kv -> kv != null)//
				.collect(Collectors.toConcurrentMap(//
						t3 -> t3._2, //
						t3 -> t3, //
						(Tuple3<AbgService, Class, Object> v1, Tuple3<AbgService, Class, Object> v2) -> {
							if (logger.isWarnEnabled()) {
								AbgService AbgService = v1._1;
								Class AbgServiceClass = v1._2;

								Class implClass1 = v1._3.getClass();
								Class implClass2 = v2._3.getClass();

								logger.warn("存在冲突 AbgService: " + AbgServiceClass.getName() + AbgService //
										+ ", 生效: " + implClass1.getName() //
										+ ", 忽略: " + implClass2.getName());
							}

							return v1;
						}));

		return AbgServiceMap.values();
	}

	@SuppressWarnings("rawtypes")
	private Tuple2<AbgService, Class> getAbgService(Object bean) {
		if (bean == null) {
			return null;
		}

		Class<?> beanClass = bean.getClass();

		if (beanClass.getAnnotation(AbgFailover.class) != null) {
			return null;
		}

		Class<?>[] interfaces = beanClass.getInterfaces();
		if (interfaces == null || interfaces.length == 0) {
			return null;
		}

		for (int i = 0; i < interfaces.length; i++) {
			Class<?> interfaceClass = interfaces[i];

			AbgService AbgService = interfaceClass.getAnnotation(AbgService.class);
			if (AbgService != null) {
				return tuple(AbgService, interfaceClass);
			}
		}

		return null;
	}

}
