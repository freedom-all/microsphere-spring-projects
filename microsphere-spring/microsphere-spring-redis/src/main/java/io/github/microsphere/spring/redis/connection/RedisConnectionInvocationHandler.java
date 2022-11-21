package io.github.microsphere.spring.redis.connection;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.interceptor.RedisCommandInterceptor;
import io.github.microsphere.spring.redis.interceptor.RedisConnectionInterceptor;
import io.github.microsphere.spring.redis.interceptor.RedisMethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static io.github.microsphere.spring.util.BeanUtils.getSortedBeans;

/**
 * {@link InvocationHandler} {@link RedisConnection}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class RedisConnectionInvocationHandler implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionInvocationHandler.class);

    private final RedisConnection rawRedisConnection;

    private final ApplicationContext context;

    private final String applicationName;

    private final String sourceBeanName;

    private final List<RedisConnectionInterceptor> redisConnectionInterceptors;

    private final List<RedisCommandInterceptor> redisCommandInterceptors;

    private final int redisConnectionInterceptorCount;

    private final int redisCommandInterceptorCount;

    private final boolean hasRedisConnectionInterceptors;

    private final boolean hasRedisCommandInterceptors;

    public RedisConnectionInvocationHandler(RedisConnection rawRedisConnection, RedisConfiguration redisConfiguration) {
        this(rawRedisConnection, redisConfiguration, null);
    }

    public RedisConnectionInvocationHandler(RedisConnection rawRedisConnection, RedisConfiguration redisConfiguration, String sourceBeanName) {
        this.rawRedisConnection = rawRedisConnection;
        this.context = redisConfiguration.getContext();
        this.applicationName = redisConfiguration.getApplicationName();
        this.sourceBeanName = sourceBeanName;
        this.redisCommandInterceptors = getRedisCommandInterceptors(context);
        this.redisConnectionInterceptors = getRedisConnectionInterceptors(context);
        this.redisCommandInterceptorCount = redisCommandInterceptors.size();
        this.redisConnectionInterceptorCount = redisConnectionInterceptors.size();
        this.hasRedisCommandInterceptors = redisCommandInterceptorCount > 0;
        this.hasRedisConnectionInterceptors = redisConnectionInterceptorCount > 0;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        beforeExecute(method, args);
        Object result = null;
        Throwable failure = null;
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            result = method.invoke(rawRedisConnection, args);
        } catch (Throwable e) {
            failure = e;
            throw e;
        } finally {
            afterExecute(method, args, result, failure);
        }
        return result;
    }

    private List<RedisCommandInterceptor> getRedisCommandInterceptors(ApplicationContext context) {
        return getSortedBeans(context, RedisCommandInterceptor.class);
    }

    private List<RedisConnectionInterceptor> getRedisConnectionInterceptors(ApplicationContext context) {
        return getSortedBeans(context, RedisConnectionInterceptor.class);
    }

    private void beforeExecute(Method method, Object[] args) {
        beforeExecute(redisConnectionInterceptors, redisConnectionInterceptorCount, hasRedisConnectionInterceptors, method, args);
        beforeExecute(redisCommandInterceptors, redisCommandInterceptorCount, hasRedisCommandInterceptors, method, args);
    }

    private void beforeExecute(List<? extends RedisMethodInterceptor> redisMethodInterceptors, int size, boolean exists, Method method, Object[] args) {
        if (exists) {
            for (int i = 0; i < size; i++) {
                RedisMethodInterceptor interceptor = redisMethodInterceptors.get(i);
                try {
                    interceptor.beforeExecute(rawRedisConnection, method, args, sourceBeanName);
                } catch (Throwable e) {
                    logger.error("beforeExecute method fails to execute, Source[bean name: '{}'], method: '{}'", interceptor.getClass().getName(), sourceBeanName, method, e);
                }
            }
        }
    }

    private void afterExecute(Method method, Object[] args, Object result, Throwable failure) {
        afterExecute(redisConnectionInterceptors, redisConnectionInterceptorCount, hasRedisConnectionInterceptors, method, args, result, failure);
        afterExecute(redisCommandInterceptors, redisCommandInterceptorCount, hasRedisCommandInterceptors, method, args, result, failure);
    }

    private void afterExecute(List<? extends RedisMethodInterceptor> redisMethodInterceptors, int size, boolean exists, Method method, Object[] args, Object result, Throwable failure) {
        if (exists) {
            for (int i = 0; i < size; i++) {
                RedisMethodInterceptor interceptor = redisMethodInterceptors.get(i);
                try {
                    interceptor.afterExecute(rawRedisConnection, method, args, result, failure, sourceBeanName);
                } catch (Throwable e) {
                    logger.error("beforeExecute method fails to execute, Source[bean name: '{}'], method: '{}'", interceptor.getClass().getName(), sourceBeanName, method, e);
                }
            }
        }
    }
}
