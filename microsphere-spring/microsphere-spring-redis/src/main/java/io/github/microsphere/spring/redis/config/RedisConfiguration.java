package io.github.microsphere.spring.redis.config;

import io.github.microsphere.spring.redis.event.RedisConfigurationPropertyChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import static io.github.microsphere.spring.redis.util.RedisConstants.COMMAND_EVENT_EXPOSED_PROPERTY_NAME;
import static io.github.microsphere.spring.redis.util.RedisConstants.DEFAULT_COMMAND_EVENT_EXPOSED;
import static io.github.microsphere.spring.redis.util.RedisConstants.DEFAULT_ENABLED;
import static io.github.microsphere.spring.redis.util.RedisConstants.ENABLED_PROPERTY_NAME;

/**
 * Redis Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisConfiguration implements ApplicationListener<RedisConfigurationPropertyChangedEvent>, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * RedisConfiguration
     * {@link RedisConfiguration} Bean Name
     */
    public static final String BEAN_NAME = "redisConfiguration";

    protected ConfigurableEnvironment environment;

    protected String applicationName;

    protected volatile boolean enabled;

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

    public ConfigurableEnvironment getEnvironment() {
        return environment;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public boolean isCommandEventExposed() {
        return isCommandEventExposed(environment);
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

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
        this.applicationName = resolveApplicationName(environment);
        setEnabled();
    }
}
