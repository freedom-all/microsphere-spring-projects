package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.connection.RedisConnectionInvocationHandler;
import io.github.microsphere.spring.redis.context.RedisContext;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * {@link RedisTemplate} Wrapper class, compatible with {@link RedisTemplate}
 *
 * @param <K> Redis Key type
 * @param <V> Redis Value type
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisTemplateWrapper<K, V> extends RedisTemplate<K, V> {

    private static final Class[] REDIS_CONNECTION_TYPES = new Class[]{RedisConnection.class};

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String beanName;

    private final RedisContext redisContext;

    public RedisTemplateWrapper(String beanName, RedisTemplate<K, V> redisTemplate, RedisContext redisContext) {
        this.beanName = beanName;
        this.redisContext = redisContext;
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
            return newProxyRedisConnection(connection, redisContext, beanName);
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
        return redisContext.isEnabled();
    }

    protected static RedisConnection newProxyRedisConnection(RedisConnection connection, RedisContext redisContext, String sourceBeanName) {
        ClassLoader classLoader = redisContext.getClassLoader();
        InvocationHandler invocationHandler = newInvocationHandler(connection, redisContext, sourceBeanName);
        return (RedisConnection) Proxy.newProxyInstance(classLoader, REDIS_CONNECTION_TYPES, invocationHandler);
    }

    private static InvocationHandler newInvocationHandler(RedisConnection connection, RedisContext redisContext, String redisTemplateBeanName) {
        return new RedisConnectionInvocationHandler(connection, redisContext, redisTemplateBeanName);
    }

    public String getBeanName() {
        return beanName;
    }

    public RedisContext getRedisContext() {
        return redisContext;
    }
}
