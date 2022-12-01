package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.context.RedisContext;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import static io.github.microsphere.spring.redis.beans.RedisTemplateWrapper.configure;
import static io.github.microsphere.spring.redis.beans.RedisTemplateWrapper.newProxyRedisConnection;


/**
 * {@link StringRedisTemplate} Wrapper class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class StringRedisTemplateWrapper extends StringRedisTemplate implements Wrapper {

    private static final Class<StringRedisTemplate> STRING_REDIS_TEMPLATE_CLASS = StringRedisTemplate.class;

    private final String beanName;

    private final StringRedisTemplate delegate;

    private final RedisContext redisContext;

    public StringRedisTemplateWrapper(String beanName, StringRedisTemplate delegate, RedisContext redisContext) {
        this.beanName = beanName;
        this.delegate = delegate;
        this.redisContext = redisContext;
        init();
    }

    private void init() {
        configure(delegate, this);
    }

    @Override
    protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
        if (isEnabled()) {
            return newProxyRedisConnection(connection, redisContext, beanName);
        }
        return connection;
    }

    public boolean isEnabled() {
        return redisContext.isEnabled();
    }

    @Override
    public <T> T unwrap(Class<T> type) throws IllegalArgumentException {
        if (STRING_REDIS_TEMPLATE_CLASS.equals(type)) {
            return (T) delegate;
        } else if (type.isInstance(this)) {
            return (T) this;
        }
        throw new IllegalArgumentException(getClass().getName() + " can't unwrap the given type '" + type.getName() + "'");
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return STRING_REDIS_TEMPLATE_CLASS.equals(type);
    }
}
