package io.github.microsphere.spring.redis.context;

import io.github.microsphere.spring.redis.annotation.EnableRedisInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;

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
            AnnotationConfigRegistry registry = (AnnotationConfigRegistry) context;
            if (isCommandEventExposed(context)) {
                registry.register(Config.class);
            } else {
                registry.register(NoExposingCommandEventConfig.class);
            }
        }
    }

    private boolean supports(ConfigurableApplicationContext context) {
        if (!(context instanceof AnnotationConfigRegistry)) {
            logger.warn("The application context [id: {}, class: {}] is not a {} type", context.getId(), context.getClass(), AnnotationConfigRegistry.class);
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

    @EnableRedisInterceptor(
            wrapRedisTemplates = DEFAULT_WRAP_REDIS_TEMPLATE_PLACEHOLDER,
            exposeCommandEvent = false
    )
    private static class NoExposingCommandEventConfig {
    }

}
