package io.github.microsphere.spring.redis.interceptor;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link RedisConnection} 拦截器
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface RedisConnectionInterceptor {

    /**
     * 拦截 {@link RedisConnection} 方法执行前
     *
     * @param redisTemplateBeanName {@link RedisTemplate} Bean 名称
     * @param redisConnection       {@link RedisConnection} 对象
     * @param method                {@link RedisConnection} 被执行 {@link Method}
     * @param args                  {@link RedisConnection} 被执行 {@link Method}参数
     * @throws Throwable 当方法实现执行异常时
     */
    void beforeExecute(String redisTemplateBeanName, RedisConnection redisConnection, Method method, Object[] args) throws Throwable;

    /**
     * 拦截 {@link RedisConnection} 方法执行后
     *
     * @param redisTemplateBeanName {@link RedisTemplate} Bean 名称
     * @param redisConnection       {@link RedisConnection} 对象
     * @param method                {@link RedisConnection} 被执行 {@link Method}
     * @param args                  {@link RedisConnection} 被执行 {@link Method}参数
     * @param result                {@link RedisConnection} 执行结果（可选）
     * @param failure               {@link Throwable} 异常信息（可选）
     * @throws Throwable 当方法实现执行异常时
     */
    void afterExecute(String redisTemplateBeanName, RedisConnection redisConnection, Method method, Object[] args,
                      Optional<Object> result, Optional<Throwable> failure) throws Throwable;
}
