package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Redis Method interceptor
 *
 * @param <T> The target type of Redis
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisMethodInterceptor<T> {

    /**
     * Intercept {@link T The target Redis instance} method before execution
     *
     * @param target                {@link T The target Redis instance}
     * @param method                {@link T The target Redis instance} executing {@link Method}
     * @param args                  {@link T The target Redis instance} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(T target, Method method, Object[] args, Optional<String> sourceBeanName) throws Throwable {
    }

    /**
     * Intercept {@link T The target Redis instance} method before execution
     *
     * @param target                {@link T The target Redis instance}
     * @param method                {@link T The target Redis instance} executing {@link Method}
     * @param args                  {@link T The target Redis instance} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(T target, Method method, Object[] args, @Nullable String sourceBeanName) throws Throwable {
        beforeExecute(target, method, args, ofNullable(sourceBeanName));
    }

    /**
     * Intercept {@link T The target Redis instance} method after execution
     *
     * @param target                {@link T The target Redis instance}
     * @param method                {@link T The target Redis instance} executing {@link Method}
     * @param args                  {@link T The target Redis instance} executing {@link Method} arguments
     * @param result                The Optional of {@link T The target Redis instance} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(T target, Method method, Object[] args, Optional<Object> result, Optional<Throwable> failure, Optional<String> sourceBeanName) throws Throwable {
    }

    /**
     * Intercept {@link T The target Redis instance} method after execution
     *
     * @param target                {@link T The target Redis instance}
     * @param method                {@link T The target Redis instance} executing {@link Method}
     * @param args                  {@link T The target Redis instance} executing {@link Method} arguments
     * @param result                The Optional of {@link T The target Redis instance} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(T target, Method method, Object[] args, @Nullable Object result, @Nullable Throwable failure, @Nullable String sourceBeanName) throws Throwable {
        afterExecute(target, method, args, ofNullable(result), ofNullable(failure), ofNullable(sourceBeanName));
    }
}
