package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * {@link RedisCommands Redis Command} interceptor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisCommandInterceptor extends RedisMethodInterceptor<RedisCommands> {

    /**
     * Intercept {@link RedisCommands Redis Command} method before execution
     *
     * @param redisCommands  {@link RedisCommands Redis Command}
     * @param method         {@link RedisCommands Redis Command} executing {@link Method}
     * @param args           {@link RedisCommands Redis Command} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(RedisCommands redisCommands, Method method, Object[] args, Optional<String> sourceBeanName) throws Throwable {
    }

    /**
     * Intercept {@link RedisCommands Redis Command} method before execution
     *
     * @param redisCommands  {@link RedisCommands Redis Command}
     * @param method         {@link RedisCommands Redis Command} executing {@link Method}
     * @param args           {@link RedisCommands Redis Command} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @throws Throwable When method implementations execute exceptions
     */
    default void beforeExecute(RedisCommands redisCommands, Method method, Object[] args, @Nullable String sourceBeanName) throws Throwable {
        beforeExecute(redisCommands, method, args, ofNullable(sourceBeanName));
    }

    /**
     * Intercept {@link RedisCommands Redis Command} method after execution
     *
     * @param redisCommands  {@link RedisCommands Redis Command}
     * @param method         {@link RedisCommands Redis Command} executing {@link Method}
     * @param args           {@link RedisCommands Redis Command} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @param result         The {@link Optional} of {@link RedisCommands Redis Command} execution result
     * @param failure        The {@link Optional} of {@link Throwable} Throwable
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(RedisCommands redisCommands, Method method, Object[] args, Optional<String> sourceBeanName, Optional<Object> result, Optional<Throwable> failure) throws Throwable {
    }

    /**
     * Intercept {@link RedisCommands Redis Command} method after execution
     *
     * @param redisCommands  {@link RedisCommands Redis Command}
     * @param method         {@link RedisCommands Redis Command} executing {@link Method}
     * @param args           {@link RedisCommands Redis Command} executing {@link Method} arguments
     * @param sourceBeanName The {@link Optional} of Source Bean Name
     * @param result         The {@link Optional} of {@link RedisCommands Redis Command} execution result
     * @param failure        The {@link Optional} of {@link Throwable} Throwable
     * @throws Throwable When method implementations execute exceptions
     */
    default void afterExecute(RedisCommands redisCommands, Method method, Object[] args, @Nullable String sourceBeanName, @Nullable Object result, @Nullable Throwable failure) throws Throwable {
        afterExecute(redisCommands, method, args, ofNullable(sourceBeanName), ofNullable(result), ofNullable(failure));
    }
}
