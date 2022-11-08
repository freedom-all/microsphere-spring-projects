package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link RedisConnection} interceptor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisConnectionInterceptor {

    /**
     * intercept {@link RedisConnection} method before execution
     *
     * @param redisTemplateBeanName {@link RedisTemplate} Bean Name
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @throws Throwable When method implementations execute exceptions
     */
    void beforeExecute(String redisTemplateBeanName, RedisConnection redisConnection, Method method, Object[] args) throws Throwable;

    /**
     * intercept {@link RedisConnection} method after execution
     *
     * @param redisTemplateBeanName {@link RedisTemplate} Bean Name
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param result                The Optional of {@link RedisConnection} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @throws Throwable When method implementations execute exceptions
     */
    void afterExecute(String redisTemplateBeanName, RedisConnection redisConnection, Method method, Object[] args,
                      Optional<Object> result, Optional<Throwable> failure) throws Throwable;
}
