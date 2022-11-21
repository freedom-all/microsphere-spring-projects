package io.github.microsphere.spring.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static io.github.microsphere.spring.redis.beans.RedisTemplateWrapperBeanPostProcessor.REDIS_TEMPLATE_BEAN_NAME;
import static io.github.microsphere.spring.redis.beans.RedisTemplateWrapperBeanPostProcessor.STRING_REDIS_TEMPLATE_BEAN_NAME;
import static io.github.microsphere.spring.redis.config.RedisConfiguration.BEAN_NAME;

/**
 * Redis Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Configuration(BEAN_NAME)
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * {@link RedisConfiguration} Bean Name
     */
    public static final String BEAN_NAME = "redisConfiguration";

    public static final String PROPERTY_NAME_PREFIX = "microsphere.redis.";

    public static final String ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "enabled";

    public static final boolean DEFAULT_ENABLED = Boolean.getBoolean(ENABLED_PROPERTY_NAME);

    public static final String FAIL_FAST_ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "fail-fast";

    public static final boolean FAIL_FAST_ENABLED = Boolean.getBoolean(System.getProperty(FAIL_FAST_ENABLED_PROPERTY_NAME, "true"));

    protected final ConfigurableApplicationContext context;

    protected final ConfigurableEnvironment environment;

    protected final String applicationName;

    protected volatile boolean enabled;

    public RedisConfiguration(ConfigurableApplicationContext context) {
        this.context = context;
        this.environment = context.getEnvironment();
        this.applicationName = resolveApplicationName(environment);
        setEnabled();
    }

    protected void setEnabled() {
        this.enabled = isEnabled(environment);
    }

    public boolean isEnabled() {
        return enabled;
    }


    public static boolean isEnabled(ApplicationContext context) {
        Environment environment = context.getEnvironment();
        return isEnabled(environment);
    }

    public static boolean isEnabled(Environment environment) {
        boolean enabled = getEnabled(environment);
        logger.debug("Microsphere Redis is {}, if {}, please configure the Spring property [{} = {}]", enabled ? "Enabled" : "Disabled", enabled ? "Disabled" : "Enabled", ENABLED_PROPERTY_NAME, !enabled);
        return enabled;
    }

    protected static boolean getEnabled(Environment environment) {
        return environment.getProperty(ENABLED_PROPERTY_NAME, boolean.class, DEFAULT_ENABLED);
    }

    protected String resolveApplicationName(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "default");
        return applicationName;
    }


    public ConfigurableApplicationContext getContext() {
        return context;
    }

    public ConfigurableEnvironment getEnvironment() {
        return environment;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public static RedisTemplate<?, ?> getRedisTemplate(ApplicationContext context, boolean isSourceFromStringTemplate) {
        if (isSourceFromStringTemplate) {
            return context.getBean(STRING_REDIS_TEMPLATE_BEAN_NAME, StringRedisTemplate.class);
        } else {
            return context.getBean(REDIS_TEMPLATE_BEAN_NAME, RedisTemplate.class);
        }
    }

    public static RedisConfiguration get(BeanFactory beanFactory) {
        return beanFactory.getBean(BEAN_NAME, RedisConfiguration.class);
    }

}
