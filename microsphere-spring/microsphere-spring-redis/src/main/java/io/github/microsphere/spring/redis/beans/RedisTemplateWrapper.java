package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.connection.RedisConnectionInvocationHandler;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.REDIS_TEMPLATE_SOURCE;

/**
 * {@link RedisTemplate} Wrapper class, compatible with {@link RedisTemplate}
 *
 * @param <K> Redis Key type
 * @param <V> Redis Value type
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisTemplateWrapper<K, V> extends RedisTemplate<K, V> implements BeanNameAware {

    private static final Class[] REDIS_CONNECTION_TYPES = new Class[]{RedisConnection.class};

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ApplicationContext context;

    private final RedisConfiguration redisConfiguration;

    private String beanName;

    public RedisTemplateWrapper(RedisTemplate<K, V> redisTemplate, ApplicationContext context) {
        this.context = context;
        this.redisConfiguration = RedisConfiguration.get(context);
        initSettings(redisTemplate);
    }

    private void initSettings(RedisTemplate<K, V> redisTemplate) {
        // Set the connection
        setConnectionFactory(redisTemplate.getConnectionFactory());
        setExposeConnection(redisTemplate.isExposeConnection());

        // Set the RedisSerializers
        setEnableDefaultSerializer(redisTemplate.isEnableDefaultSerializer());
        setDefaultSerializer(redisTemplate.getDefaultSerializer());
        setKeySerializer(redisTemplate.getKeySerializer());
        setValueSerializer(redisTemplate.getValueSerializer());
        setHashKeySerializer(redisTemplate.getHashKeySerializer());
        setHashValueSerializer(redisTemplate.getHashValueSerializer());
        setStringSerializer(redisTemplate.getStringSerializer());
    }

    @Override
    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        if (isEnabled()) {
            return newProxyRedisConnection(connection, redisConfiguration, beanName, REDIS_TEMPLATE_SOURCE);
        }
        return connection;
    }

    @Nullable
    @Override
    protected <T> T postProcessResult(@Nullable T result, RedisConnection conn, boolean existingConnection) {
        if (isEnabled()) {
            // Clear method parameter data
            ParametersHolder.clear();
            logger.debug("Method parameter metadata has been cleared");
        }
        return result;
    }

    public boolean isEnabled() {
        return redisConfiguration.isEnabled();
    }

    protected static RedisConnection newProxyRedisConnection(RedisConnection connection,
                                                             RedisConfiguration redisConfiguration,
                                                             String redisTemplateBeanName, byte sourceFrom) {
        ApplicationContext context = redisConfiguration.getContext();
        ClassLoader classLoader = context.getClassLoader();
        InvocationHandler invocationHandler = newInvocationHandler(connection, redisConfiguration, redisTemplateBeanName, sourceFrom);
        return (RedisConnection) Proxy.newProxyInstance(classLoader, REDIS_CONNECTION_TYPES, invocationHandler);
    }

    private static InvocationHandler newInvocationHandler(RedisConnection connection,
                                                          RedisConfiguration redisConfiguration,
                                                          String redisTemplateBeanName, byte sourceFrom) {
        return new RedisConnectionInvocationHandler(connection, redisConfiguration, redisTemplateBeanName, sourceFrom);
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}
