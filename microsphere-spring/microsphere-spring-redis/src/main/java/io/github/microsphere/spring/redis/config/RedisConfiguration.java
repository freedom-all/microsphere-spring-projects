package io.github.microsphere.spring.redis.config;

import io.github.microsphere.spring.util.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Redis Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * {@link RedisConfiguration} Bean Name
     */
    public static final String BEAN_NAME = "redisConfiguration";

    public static final String PROPERTY_NAME_PREFIX = "microsphere.redis.";

    public static final String ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "enabled";

    public static final boolean DEFAULT_ENABLED = Boolean.getBoolean(ENABLED_PROPERTY_NAME);

    /**
     * {@link RedisTemplate} Bean Name
     */
    public static final String REDIS_TEMPLATE_BEAN_NAME = "redisTemplate";

    /**
     * {@link StringRedisTemplate} Bean Name
     */
    public static final String STRING_REDIS_TEMPLATE_BEAN_NAME = "stringRedisTemplate";

    /**
     * In addition to the default {@link #REDIS_TEMPLATE_BEAN_NAME} and {@link #STRING_REDIS_TEMPLATE_BEAN_NAME} Bean Name,
     * Attached {@link RedisTemplate} or {@link StringRedisTemplate} list of Bean names
     */
    public static final String ADDITIONAL_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "additional-redis-templates";

    public static final String FAIL_FAST_ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "fail-fast";

    public static final boolean FAIL_FAST_ENABLED = Boolean.parseBoolean(System.getProperty(FAIL_FAST_ENABLED_PROPERTY_NAME, "true"));

    /**
     * {@link RedisTemplate} Source identification
     */
    public static final byte REDIS_TEMPLATE_SOURCE = 1;

    /**
     * {@link StringRedisTemplate} source identification
     */
    public static final byte STRING_REDIS_TEMPLATE_SOURCE = 2;

    /**
     * The custom {@link RedisTemplate} source identification
     * TODO: customization is not supported {@link RedisTemplate}
     */
    public static final byte CUSTOMIZED_REDIS_TEMPLATE_SOURCE = 3;

    protected final ConfigurableApplicationContext context;

    protected final ConfigurableEnvironment environment;

    protected final String applicationName;

    protected final List<String> redisTemplateBeanNames;

    protected volatile boolean enabled;

    public RedisConfiguration(ConfigurableApplicationContext context) {
        this.context = context;
        this.environment = context.getEnvironment();
        this.applicationName = resolveApplicationName(environment);
        this.redisTemplateBeanNames = resolveRestTemplateBeanNames(context);
        setEnabled();
    }

    protected void setEnabled() {
        this.enabled = isEnabled(environment);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the attached {@link RedisTemplate} or {@link StringRedisTemplate} Bean Name list
     * (except the default {@link #REDIS_TEMPLATE_BEAN_NAME} and {@link #STRING_REDIS_TEMPLATE_BEAN_NAME} outside the Bean Name)
     *
     * @param environment {@link Environment}
     * @return If no configuration is found, an empty list is returned
     */
    public List<String> getAdditionalRedisTemplateBeanNames(Environment environment) {
        return unmodifiableList(environment.getProperty(ADDITIONAL_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME, List.class, emptyList()));
    }

    /**
     * Get {@link RedisConfiguration}
     *
     * @param beanFactory {@link BeanFactory}
     * @return Does not return <code>null<code>
     */
    public static RedisConfiguration get(BeanFactory beanFactory) {
        return beanFactory.getBean(BEAN_NAME, RedisConfiguration.class);
    }

    public static boolean isEnabled(ApplicationContext context) {
        Environment environment = context.getEnvironment();
        return isEnabled(environment);
    }

    public static boolean isEnabled(Environment environment) {
        boolean enabled = getEnabled(environment);
        logger.debug("Microsphere Redis is {}, if {}, please configure the Spring property [{} = {}]",
                enabled ? "Enabled" : "Disabled",
                enabled ? "Disabled" : "Enabled",
                ENABLED_PROPERTY_NAME,
                !enabled);
        return enabled;
    }

    protected static boolean getEnabled(Environment environment) {
        return environment.getProperty(ENABLED_PROPERTY_NAME, boolean.class, DEFAULT_ENABLED);
    }

    protected String resolveApplicationName(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name", "default");
        return applicationName;
    }

    private List<String> resolveRestTemplateBeanNames(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        String[] redisTemplateBeanNames = BeanUtils.getBeanNames(beanFactory, RedisTemplate.class);
        return unmodifiableList(Arrays.asList(redisTemplateBeanNames));
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

    public List<String> getRedisTemplateBeanNames() {
        return redisTemplateBeanNames;
    }

    public static RedisTemplate<?, ?> getRedisTemplate(ApplicationContext context, boolean isSourceFromStringTemplate) {
        if (isSourceFromStringTemplate) {
            return context.getBean(STRING_REDIS_TEMPLATE_BEAN_NAME, StringRedisTemplate.class);
        } else {
            return context.getBean(REDIS_TEMPLATE_BEAN_NAME, RedisTemplate.class);
        }
    }

}
