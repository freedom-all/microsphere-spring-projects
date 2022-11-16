package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * {@link RedisCommands} interceptor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisCommandsInterceptor extends RedisMethodInterceptor<RedisCommands> {

    /**
     * Intercept {@link RedisCommands} method before execution
     *
     * @param redisCommands         {@link RedisCommands}
     * @param method                {@link RedisCommands} executing {@link Method}
     * @param args                  {@link RedisCommands} executing {@link Method} arguments
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void beforeExecute(RedisCommands redisCommands, Method method, Object[] args, Optional<String> redisTemplateBeanName) throws Throwable;

    /**
     * Intercept {@link RedisCommands} method before execution
     *
     * @param redisCommands         {@link RedisCommands}
     * @param method                {@link RedisCommands} executing {@link Method}
     * @param args                  {@link RedisCommands} executing {@link Method} arguments
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(RedisCommands redisCommands, Method method, Object[] args, @Nullable String redisTemplateBeanName) throws Throwable {
        beforeExecute(redisCommands, method, args, ofNullable(redisTemplateBeanName));
    }

    /**
     * Intercept {@link RedisCommands} method after execution
     *
     * @param redisCommands         {@link RedisCommands}
     * @param method                {@link RedisCommands} executing {@link Method}
     * @param args                  {@link RedisCommands} executing {@link Method} arguments
     * @param result                The Optional of {@link RedisCommands} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    void afterExecute(RedisCommands redisCommands, Method method, Object[] args, Optional<Object> result, Optional<Throwable> failure, Optional<String> redisTemplateBeanName) throws Throwable;

    /**
     * Intercept {@link RedisCommands} method after execution
     *
     * @param redisCommands         {@link RedisCommands}
     * @param method                {@link RedisCommands} executing {@link Method}
     * @param args                  {@link RedisCommands} executing {@link Method} arguments
     * @param result                The Optional of {@link RedisCommands} execution result
     * @param failure               The Optional of {@link Throwable} Throwable
     * @param redisTemplateBeanName The Optional of {@link RedisTemplate} Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(RedisCommands redisCommands, Method method, Object[] args, @Nullable Object result, @Nullable Throwable failure, @Nullable String redisTemplateBeanName) throws Throwable {
        afterExecute(redisCommands, method, args, ofNullable(result), ofNullable(failure), ofNullable(redisTemplateBeanName));
    }
}
