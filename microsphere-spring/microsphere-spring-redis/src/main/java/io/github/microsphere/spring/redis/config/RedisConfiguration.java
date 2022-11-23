package io.github.microsphere.spring.redis.config;

import io.github.microsphere.spring.redis.event.RedisConfigurationPropertyChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.BEAN_NAME;
import static io.github.microsphere.spring.redis.util.RedisConstants.COMMAND_EVENT_EXPOSED_PROPERTY_NAME;
import static io.github.microsphere.spring.redis.util.RedisConstants.DEFAULT_COMMAND_EVENT_EXPOSED;
import static io.github.microsphere.spring.redis.util.RedisConstants.DEFAULT_ENABLED;
import static io.github.microsphere.spring.redis.util.RedisConstants.ENABLED_PROPERTY_NAME;
import static io.github.microsphere.spring.redis.util.RedisConstants.REDIS_TEMPLATE_BEAN_NAME;
import static io.github.microsphere.spring.redis.util.RedisConstants.STRING_REDIS_TEMPLATE_BEAN_NAME;

/**
 * Redis Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Configuration(BEAN_NAME)
public class RedisConfiguration implements ApplicationListener<RedisConfigurationPropertyChangedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * {@link RedisConfiguration} Bean Name
     */
    public static final String BEAN_NAME = "redisConfiguration";

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

    @Override
    public void onApplicationEvent(RedisConfigurationPropertyChangedEvent event) {
        if (event.hasProperty(ENABLED_PROPERTY_NAME)) {
            setEnabled();
        }
    }

    public void setEnabled() {
        this.enabled = isEnabled(environment);
    }

    public boolean isEnabled() {
        return enabled;
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

    public boolean isCommandEventExposed() {
        return isCommandEventExposed(context);
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

    public static boolean isCommandEventExposed(ApplicationContext context) {
        return isCommandEventExposed(context.getEnvironment());
    }

    public static boolean isCommandEventExposed(Environment environment) {
        String name = COMMAND_EVENT_EXPOSED_PROPERTY_NAME;
        boolean exposed = environment.getProperty(name, boolean.class, DEFAULT_COMMAND_EVENT_EXPOSED);
        logger.debug("Microsphere Redis Command Event is {}exposed, if {}, please configure the Spring property [{} = {}]", exposed ? "" : "not ", exposed ? "Disabled" : "Exposed", name, !exposed);
        return exposed;
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
