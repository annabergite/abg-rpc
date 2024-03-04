package rpc.abg.boot;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;

import rpc.abg.annotation.abgFailover;
import rpc.abg.annotation.abgService;
import rpc.abg.client.abgClient;
import rpc.abg.config.client.ClientConfig;
import rpc.abg.util.ReflectUtils;

public class abgClientStarter implements BeanFactoryPostProcessor, BeanPostProcessor, Ordered {
	private static final Log logger = LogFactory.getLog(abgClientStarter.class);

	private ConfigurableListableBeanFactory beanFactory;
	private abgClient abgClient;

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;

		try {
			ClientConfig clientConfig = ClientConfig.parse("abg-client.conf");
			abgClient = new abgClient(clientConfig);
		} catch (com.typesafe.config.ConfigException configException) {
			if (logger.isErrorEnabled()) {
				logger.error("abg-client.conf 格式错误，无法开启abgClient!", configException);
			}

			throw configException;
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("类路径中找不到 abg-client.conf，无法开启abgClient!", e);
			}

			throw e;
		}

		Collection<Class<?>> abgClassList = extractabgServiceClassList(beanFactory);

		for (Class<?> abgClass : abgClassList) {
			registerabgService(abgClass);
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean == null) {
			return bean;
		}

		tryInjectabgServiceField(bean);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean == null) {
			return bean;
		}

		if (bean instanceof abgClientAware) {
			((abgClientAware) bean).setabgClient(abgClient);
		}

		Class<?> clazz = bean.getClass();
		abgFailover abgFailover = clazz.getAnnotation(abgFailover.class);

		if (abgFailover == null) {
			return bean;
		}

		if (logger.isInfoEnabled()) {
			logger.info("扫描到Failover实例，重置abgFailover: " + clazz.getName() + abgFailover);
		}

		abgClient.setFailover(abgFailover.service(), bean);

		return bean;
	}

	@PreDestroy
	public void close() {
		if (abgClient == null) {
			return;
		}

		try {
			abgClient.close();
		} catch (Throwable e) {
			if (logger.isErrorEnabled()) {
				logger.error("abgClient关闭失败!", e);
			}
		}
	}

	// 具体实现，下面的不用关注

	private void registerabgService(Class<?> abgClass) {
		if (abgClient.getService(abgClass) != null) {
			return;
		}

		String[] exists = beanFactory.getBeanNamesForType(abgClass);
		// 如果已经注册则忽略，防止autowired byType冲突
		if (exists != null && exists.length > 0) {
			if (logger.isInfoEnabled()) {
				logger.info("spring中已存在: " + abgClass.getName() + ", 将不注册相应的远程服务对象, 通过反射对需要的字段直接赋值");
			}

			return;
		}

		String beanName = UPPER_CAMEL.to(LOWER_CAMEL, abgClass.getSimpleName());

		if (logger.isWarnEnabled() && beanFactory.containsBean(beanName)) {
			String existClass = null;

			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition != null) {
				existClass = beanDefinition.getBeanClassName();
			}

			if (existClass == null) {
				try {
					existClass = beanFactory.getBean(beanName).getClass().getName();
				} catch (Throwable t) {
				}
			}

			if (existClass != null) {
				String oldBeanName = beanName;
				beanName = abgClass.getName();

				logger.warn("spring中存在冲突的类名: [" + oldBeanName + "] " //
						+ existClass + "(existed), " + abgClass.getName() + "(current)" //
						+ ", 使用类全名称注册: " + beanName//
						+ ", 将只能通过 byType 或者 @Qualifier(\"" + beanName + "\") 使用");
			}
		}

		abgClient.register(abgClass);
		abgClient.setFailover(abgClass, null);
		Object serviceBean = abgClient.getService(abgClass);

		// 无法设置为primary，要防止autowired byType冲突
		beanFactory.registerSingleton(beanName, serviceBean);

		if (logger.isInfoEnabled()) {
			logger.info("spring中注册远程服务: " + beanName + "@" + abgClass.getName());
		}
	}

	private Collection<Class<?>> extractabgServiceClassList(ConfigurableListableBeanFactory beanFactory) {
		LocalDateTime startTime = LocalDateTime.now();
		Set<Class<?>> abgServiceSet = new HashSet<>();
		String[] beanNames = beanFactory.getBeanDefinitionNames();

		for (int i = 0; i < beanNames.length; i++) {
			String beanName = beanNames[i];
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			String beanClassName = beanDefinition.getBeanClassName();

			extractabgServiceClass(abgServiceSet, beanClassName);
		}

		if (logger.isInfoEnabled()) {
			LocalDateTime finishTime = LocalDateTime.now();
			Duration duration = Duration.between(startTime, finishTime);

			String abgServiceString = abgServiceSet//
					.stream()//
					.map(clazz -> clazz.getName())//
					.collect(Collectors.joining(",", "[", "]"));

			logger.info("扫描到abgService: " + abgServiceString);
			logger.info("扫描abgService耗时: " + duration);
		}

		return abgServiceSet;
	}

	private void extractabgServiceClass(Set<Class<?>> abgServiceSet, String beanClassName) {
		if (beanClassName == null || beanClassName.startsWith("org.springframework.")) {
			return;
		}

		Class<?> beanClass;
		try {
			beanClass = Class.forName(beanClassName, false, beanFactory.getBeanClassLoader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		Collection<Class<?>> allDependClass = ReflectUtils.getAllDependClass(beanClass, clazz -> {
			if (!clazz.isInterface()) {// 只支持接口
				return false;
			}

			abgService abgService = clazz.getAnnotation(abgService.class);

			if (abgService == null) {
				return false;
			}

			return true;
		});

		abgServiceSet.addAll(allDependClass);
	}

	private void tryInjectabgServiceField(Object bean) {
		if (bean == null) {
			return;
		}

		Class<?> beanClass = bean.getClass();

		while (beanClass != Object.class) {
			tryInjectabgServiceField(beanClass, bean);
			beanClass = beanClass.getSuperclass();
		}
	}

	private void tryInjectabgServiceField(Class<?> beanClass, Object bean) {
		Field[] fields = beanClass.getDeclaredFields();
		for (int j = 0; j < fields.length; j++) {
			Field field = fields[j];
			Class<?> fieldClass = field.getType();

			if (!fieldClass.isInterface()) {// 只支持接口
				continue;
			}

			abgService abgService = fieldClass.getAnnotation(abgService.class);

			if (abgService == null) {
				continue;
			}

			Object serviceBean = abgClient.getService(fieldClass);
			if (serviceBean == null) {
				abgClient.register(fieldClass);
				abgClient.setFailover(fieldClass, null);

				serviceBean = abgClient.getService(fieldClass);
			}

			try {
				field.setAccessible(true);
				field.set(bean, serviceBean);
			} catch (Throwable t) {
				if (logger.isErrorEnabled()) {
					logger.error("手动注入abgService失败，" + field, t);
				}

				continue;
			}
		}
	}
}
