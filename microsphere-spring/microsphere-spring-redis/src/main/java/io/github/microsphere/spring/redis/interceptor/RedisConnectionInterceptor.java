package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisConnection;
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
     * @param redisConnection {@link RedisConnection}
     * @param method          {@link RedisConnection} executing {@link Method}
     * @param args            {@link RedisConnection} executing {@link Method} arguments
     * @param sourceBeanName  The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void beforeExecute(RedisConnection redisConnection, Method method, Object[] args, Optional<String> sourceBeanName);

    /**
     * Intercept {@link RedisConnection} method before execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(RedisConnection redisConnection, Method method, Object[] args, @Nullable String sourceBeanName) throws Throwable {
        beforeExecute(redisConnection, method, args, ofNullable(sourceBeanName));
    }

    /**
     * Intercept {@link RedisConnection} method after execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param result                The {@link Optional} of {@link RedisConnection} execution result
     * @param failure               The {@link Optional} of {@link Throwable} Throwable
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void afterExecute(RedisConnection redisConnection, Method method, Object[] args, Optional<Object> result, Optional<Throwable> failure, Optional<String> sourceBeanName) throws Throwable;

    /**
     * Intercept {@link RedisConnection} method after execution
     *
     * @param redisConnection       {@link RedisConnection}
     * @param method                {@link RedisConnection} executing {@link Method}
     * @param args                  {@link RedisConnection} executing {@link Method} arguments
     * @param result                The {@link Optional} of {@link RedisConnection} execution result
     * @param failure               The {@link Optional} of {@link Throwable} Throwable
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(RedisConnection redisConnection, Method method, Object[] args, @Nullable Object result, @Nullable Throwable failure, @Nullable String sourceBeanName) throws Throwable {
        afterExecute(redisConnection, method, args, ofNullable(result), ofNullable(failure), ofNullable(sourceBeanName));
    }
}
