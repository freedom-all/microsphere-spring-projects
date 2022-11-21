package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;


/**
 * {@link StringRedisTemplate} Wrapper class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class StringRedisTemplateWrapper extends StringRedisTemplate {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String beanName;

    private final RedisConfiguration redisConfiguration;

    public StringRedisTemplateWrapper(String beanName, StringRedisTemplate stringRedisTemplate, RedisConfiguration redisConfiguration) {
        this.beanName = beanName;
        this.redisConfiguration = redisConfiguration;
        initSettings(stringRedisTemplate);
    }

    private void initSettings(StringRedisTemplate stringRedisTemplate) {
        // Set up the connection
        setConnectionFactory(stringRedisTemplate.getConnectionFactory());
        setExposeConnection(stringRedisTemplate.isExposeConnection());

        // Set the RedisSerializers
        setEnableDefaultSerializer(stringRedisTemplate.isEnableDefaultSerializer());
        setDefaultSerializer(stringRedisTemplate.getDefaultSerializer());
        setKeySerializer(stringRedisTemplate.getKeySerializer());
        setValueSerializer(stringRedisTemplate.getValueSerializer());
        setHashKeySerializer(stringRedisTemplate.getHashKeySerializer());
        setHashValueSerializer(stringRedisTemplate.getHashValueSerializer());
        setStringSerializer(stringRedisTemplate.getStringSerializer());
    }

    @Override
    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        if (isEnabled()) {
            return RedisTemplateWrapper.newProxyRedisConnection(connection, redisConfiguration, beanName);
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

}
