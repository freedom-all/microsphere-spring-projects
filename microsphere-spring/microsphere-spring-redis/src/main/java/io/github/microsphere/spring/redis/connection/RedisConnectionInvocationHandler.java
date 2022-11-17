package io.github.microsphere.spring.redis.connection;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.interceptor.RedisCommandsInterceptor;
import io.github.microsphere.spring.redis.interceptor.RedisConnectionInterceptor;
import io.github.microsphere.spring.redis.interceptor.RedisMethodInterceptor;
import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.metadata.ParameterMetadata;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static io.github.microsphere.spring.redis.metadata.MethodMetadataRepository.getParameterMetadataList;
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

    private final RedisConfiguration redisConfiguration;

    private final String applicationName;

    private final String redisTemplateBeanName;

    private final byte sourceFrom;

    private final List<RedisConnectionInterceptor> redisConnectionInterceptors;

    private final List<RedisCommandsInterceptor> redisCommandsInterceptors;

    private final int redisConnectionInterceptorsSize;

    private final int redisCommandsInterceptorsSize;

    private final boolean hasRedisConnectionInterceptors;

    private final boolean hasRedisCommandsInterceptors;

    public RedisConnectionInvocationHandler(RedisConnection rawRedisConnection, RedisConfiguration redisConfiguration, String redisTemplateBeanName, byte sourceFrom) {
        this.rawRedisConnection = rawRedisConnection;
        this.redisConfiguration = redisConfiguration;
        this.context = redisConfiguration.getContext();
        this.applicationName = redisConfiguration.getApplicationName();
        this.redisTemplateBeanName = redisTemplateBeanName;
        this.sourceFrom = sourceFrom;
        this.redisCommandsInterceptors = getRedisCommandInterceptors(context);
        this.redisConnectionInterceptors = getRedisConnectionInterceptors(context);
        this.redisCommandsInterceptorsSize = redisCommandsInterceptors.size();
        this.redisConnectionInterceptorsSize = redisConnectionInterceptors.size();
        this.hasRedisCommandsInterceptors = redisCommandsInterceptorsSize > 0;
        this.hasRedisConnectionInterceptors = redisConnectionInterceptorsSize > 0;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Object result = null;

        List<ParameterMetadata> parameterMetadataList = getParameterMetadataList(method);

        if (parameterMetadataList != null) { // The current method is to copy the Redis command
            // Initializes the method parameter data
            ParametersHolder.init(parameterMetadataList, args);
            // Invoke the Redis command method
            result = doInvoke(method, args);
            // Publishes the Redis command method event
            publishRedisCommandEvent(method, args);
        } else {
            // Invoke the Redis command method
            result = doInvoke(method, args);
        }

        return result;
    }

    private List<RedisCommandsInterceptor> getRedisCommandInterceptors(ApplicationContext context) {
        return getSortedBeans(context, RedisCommandsInterceptor.class);
    }

    private List<RedisConnectionInterceptor> getRedisConnectionInterceptors(ApplicationContext context) {
        return getSortedBeans(context, RedisConnectionInterceptor.class);
    }

    private Object doInvoke(Method method, Object[] args) throws Throwable {
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

    private void beforeExecute(Method method, Object[] args) {
        beforeExecute(redisConnectionInterceptors, redisConnectionInterceptorsSize, hasRedisConnectionInterceptors, method, args);
        beforeExecute(redisCommandsInterceptors, redisCommandsInterceptorsSize, hasRedisCommandsInterceptors, method, args);
    }

    private void beforeExecute(List<? extends RedisMethodInterceptor> redisMethodInterceptors, int size, boolean exists, Method method, Object[] args) {
        if (exists) {
            for (int i = 0; i < size; i++) {
                RedisMethodInterceptor interceptor = redisMethodInterceptors.get(i);
                try {
                    interceptor.beforeExecute(rawRedisConnection, method, args, redisTemplateBeanName);
                } catch (Throwable e) {
                    logger.error("beforeExecute method fails to execute, RedisTemplate[bean name: '{}'], method: '{}'", interceptor.getClass().getName(), redisTemplateBeanName, method, e);
                }
            }
        }
    }

    private void afterExecute(Method method, Object[] args, Object result, Throwable failure) {
        afterExecute(redisConnectionInterceptors, redisCommandsInterceptorsSize, hasRedisConnectionInterceptors, method, args, result, failure);
    }

    private void afterExecute(List<? extends RedisMethodInterceptor> redisMethodInterceptors, int size, boolean exists, Method method, Object[] args, Object result, Throwable failure) {
        if (exists) {
            for (int i = 0; i < size; i++) {
                RedisMethodInterceptor interceptor = redisMethodInterceptors.get(i);
                try {
                    interceptor.afterExecute(rawRedisConnection, method, args, result, failure, redisTemplateBeanName);
                } catch (Throwable e) {
                    logger.error("beforeExecute method fails to execute, RedisTemplate[bean name: '{}'], method: '{}'", interceptor.getClass().getName(), redisTemplateBeanName, method, e);
                }
            }
        }
    }

    private void publishRedisCommandEvent(Method method, Object[] args) {
        RedisCommandEvent redisCommandEvent = createRedisCommandEvent(method, args);
        if (redisCommandEvent != null) {
            // Event handling allows exceptions to be thrown
            context.publishEvent(redisCommandEvent);
        }
    }

    private RedisCommandEvent createRedisCommandEvent(Method method, Object[] args) {
        RedisCommandEvent redisCommandEvent = null;
        try {
            Parameter[] parameters = ParametersHolder.bulkGet(args);
            redisCommandEvent = new RedisCommandEvent(method, parameters, sourceFrom, applicationName);
            redisCommandEvent.setSourceBeanName(redisTemplateBeanName);
        } catch (Throwable e) {
            logger.error("Redis failed to create a command method event.", method, e);
        }
        return redisCommandEvent;
    }
}
