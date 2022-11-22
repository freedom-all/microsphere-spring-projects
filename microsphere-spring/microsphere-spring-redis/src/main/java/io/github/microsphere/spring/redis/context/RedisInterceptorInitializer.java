package io.github.microsphere.spring.redis.context;

import io.github.microsphere.spring.redis.annotation.EnableRedisInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigRegistry;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.github.microsphere.spring.redis.beans.RedisTemplateWrapperBeanPostProcessor.WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME;
import static io.github.microsphere.spring.redis.config.RedisConfiguration.isCommandEventExposed;
import static io.github.microsphere.spring.redis.config.RedisConfiguration.isEnabled;

/**
 * Redis Interceptor Initializer
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisInterceptorInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(RedisInterceptorInitializer.class);

    private static final String DEFAULT_WRAP_REDIS_TEMPLATE_PLACEHOLDER = "${" + WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME + ":}";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (supports(context)) {
            registerConfiguration(context);
        }
    }

    private void registerConfiguration(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        ConfigurableEnvironment environment = context.getEnvironment();
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry, environment);
        if (isCommandEventExposed(context)) {
            reader.register(Config.class);
        } else {
            reader.register(NoExposingCommandEventConfig.class);
        }
    }

    private boolean supports(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (!(beanFactory instanceof BeanDefinitionRegistry)) {
            logger.warn("The application context [id: {}, class: {}]'s BeanFactory[class : {}] is not a {} type", context.getId(), context.getClass(), beanFactory.getClass(), AnnotationConfigRegistry.class);
            return false;
        }
        if (!isEnabled(context)) {
            return false;
        }
        return true;
    }

    @EnableRedisInterceptor(wrapRedisTemplates = DEFAULT_WRAP_REDIS_TEMPLATE_PLACEHOLDER)
    private static class Config {
    }

    @EnableRedisInterceptor(wrapRedisTemplates = DEFAULT_WRAP_REDIS_TEMPLATE_PLACEHOLDER, exposeCommandEvent = false)
    private static class NoExposingCommandEventConfig {
    }

}
