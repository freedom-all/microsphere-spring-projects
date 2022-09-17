package io.github.microsphere.spring.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * Redis 配置
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    /**
     * {@link RedisConfiguration} Bean 名称
     */
    public static final String BEAN_NAME = "redisSyncConfiguration";

    public static final String PROPERTY_NAME_PREFIX = "microsphere.redis.";

    public static final String ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "enabled";

    public static final boolean DEFAULT_ENABLED = Boolean.getBoolean(ENABLED_PROPERTY_NAME);

    /**
     * {@link RedisTemplate} Bean 名称
     */
    public static final String REDIS_TEMPLATE_BEAN_NAME = "redisTemplate";

    /**
     * {@link StringRedisTemplate} Bean 名称
     */
    public static final String STRING_REDIS_TEMPLATE_BEAN_NAME = "stringRedisTemplate";

    /**
     * 除默认 {@link #REDIS_TEMPLATE_BEAN_NAME} 和 {@link #STRING_REDIS_TEMPLATE_BEAN_NAME} Bean 名称之外，附加的 {@link RedisTemplate}
     * 或 {@link StringRedisTemplate} Bean 名称列表
     */
    public static final String ADDITIONAL_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "additional-redis-templates";

    public static final String FAIL_FAST_ENABLED_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "fail-fast";

    public static final boolean FAIL_FAST_ENABLED = Boolean.parseBoolean(System.getProperty(FAIL_FAST_ENABLED_PROPERTY_NAME, "true"));

    /**
     * {@link RedisTemplate} 来源标识
     */
    public static final byte REDIS_TEMPLATE_SOURCE = 1;

    /**
     * {@link StringRedisTemplate} 来源标识
     */
    public static final byte STRING_REDIS_TEMPLATE_SOURCE = 2;

    /**
     * 自定义 {@link RedisTemplate} 来源标识
     * TODO: 目前尚未支持自定义 {@link RedisTemplate}
     */
    public static final byte CUSTOMIZED_REDIS_TEMPLATE_SOURCE = 3;

    private final ConfigurableApplicationContext context;

    private final Environment environment;

    private final String applicationName;

    private volatile boolean enabled;

    public RedisConfiguration(ConfigurableApplicationContext context) {
        this.context = context;
        this.environment = context.getEnvironment();
        this.applicationName = getApplicationName(environment);
        setEnabled();
    }

    public String getApplicationName() {
        return applicationName;
    }

    private void setEnabled() {
        this.enabled = getEnabled(environment);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取附加的 {@link RedisTemplate} 或 {@link StringRedisTemplate} Bean 名称列表（除默认 {@link #REDIS_TEMPLATE_BEAN_NAME}
     * 和 {@link #STRING_REDIS_TEMPLATE_BEAN_NAME} Bean 名称外）
     *
     * @param environment {@link Environment}
     * @return 如果没有找到配置，返回空列表
     */
    public List<String> getAdditionalRedisTemplateBeanNames(Environment environment) {
        return unmodifiableList(environment.getProperty(ADDITIONAL_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME, List.class, emptyList()));
    }

    /**
     * 获取 {@link RedisConfiguration}
     *
     * @param beanFactory {@link BeanFactory}
     * @return 不会返回 <code>null</code>
     */
    public static RedisConfiguration get(BeanFactory beanFactory) {
        return beanFactory.getBean(BEAN_NAME, RedisConfiguration.class);
    }

    public static boolean isEnabled(ApplicationContext context) {
        Environment environment = context.getEnvironment();
        boolean enabled = getEnabled(environment);
        logger.debug("应用上下文[id: {}] {} Microsphere Redis ，若需{} ，请配置 Spring 属性[{} = {}]", context.getId(),
                enabled ? "开启" : "关闭",
                enabled ? "关闭" : "开启",
                ENABLED_PROPERTY_NAME,
                !enabled);
        return enabled;
    }

    protected static boolean getEnabled(Environment environment) {
        return environment.getProperty(ENABLED_PROPERTY_NAME, boolean.class, DEFAULT_ENABLED);
    }

    protected static String getApplicationName(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name");
        if (!StringUtils.hasText(applicationName)) {
            // 使用 Apollo ID 作为应用名称
            applicationName = environment.getProperty("app.id", "default");
        }
        return applicationName;
    }

    public static RedisTemplate<?, ?> getRedisTemplate(ApplicationContext context, boolean isSourceFromStringTemplate) {
        if (isSourceFromStringTemplate) {
            return context.getBean(STRING_REDIS_TEMPLATE_BEAN_NAME, StringRedisTemplate.class);
        } else {
            return context.getBean(REDIS_TEMPLATE_BEAN_NAME, RedisTemplate.class);
        }
    }

}
