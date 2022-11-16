package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * {@link RedisConnection} interceptor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisConnectionInterceptor extends RedisMethodInterceptor<RedisConnection> {

    /**
     * Intercept {@link RedisConnection} method before execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void beforeExecute(RedisConnection redisConnection, Method method, Object[] args, Optional<String> redisTemplateBeanName);

    /**
     * Intercept {@link RedisConnection} method before execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(RedisConnection redisConnection, Method method, Object[] args, @Nullable String redisTemplateBeanName) throws Throwable {
        beforeExecute(redisConnection, method, args, ofNullable(redisTemplateBeanName));
    }

    /**
     * Intercept {@link RedisConnection} method after execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param result                The Optional of {@link RedisConnection} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void afterExecute(RedisConnection redisConnection, Method method, Object[] args, Optional<Object> result, Optional<Throwable> failure, Optional<String> redisTemplateBeanName) throws Throwable;

    /**
     * Intercept {@link RedisConnection} method after execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param result                The Optional of {@link RedisConnection} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(RedisConnection redisConnection, Method method, Object[] args, @Nullable Object result, @Nullable Throwable failure, @Nullable String redisTemplateBeanName) throws Throwable {
        afterExecute(redisConnection, method, args, ofNullable(result), ofNullable(failure), ofNullable(redisTemplateBeanName));
    }
}
