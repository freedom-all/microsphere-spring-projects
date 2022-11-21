package io.github.microsphere.spring.redis.context;

import io.github.microsphere.spring.redis.beans.RedisTemplateWrapperBeanPostProcessor;
import io.github.microsphere.spring.redis.config.RedisConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigRegistry;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.isEnabled;

/**
 * Redis Initializer
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(RedisInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        if (shouldInitialization(context)) {

            RedisConfiguration redisConfiguration = createRedisConfiguration(context);

            initializeRedisTemplateWrapperBeanPostProcessor(context, redisConfiguration);
        }
    }

    private boolean shouldInitialization(ConfigurableApplicationContext context) {
        if (!(context instanceof AnnotationConfigRegistry)) {
            logger.warn("The application context [id: {}, class: {}] is not a {} type", context.getId(), context.getClass(), AnnotationConfigRegistry.class);
            return false;
        }
        if (!isEnabled(context)) {
            return false;
        }
        return true;
    }

    protected RedisConfiguration createRedisConfiguration(ConfigurableApplicationContext context) {
        return new RedisConfiguration(context);
    }

    private void initializeRedisTemplateWrapperBeanPostProcessor(ConfigurableApplicationContext context, RedisConfiguration redisConfiguration) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.addBeanPostProcessor(new RedisTemplateWrapperBeanPostProcessor(redisConfiguration));
        logger.debug("Application context [id: {}] initialized {}", RedisTemplateWrapperBeanPostProcessor.class.getName());
    }
}
