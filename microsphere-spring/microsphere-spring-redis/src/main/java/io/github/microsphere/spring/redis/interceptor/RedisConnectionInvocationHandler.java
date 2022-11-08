package io.github.microsphere.spring.redis.interceptor;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.metadata.ParameterMetadata;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.redis.connection.RedisConnection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.microsphere.spring.redis.metadata.MethodMetadataRepository.getParameterMetadataList;
import static java.util.Optional.ofNullable;
import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

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

    private final boolean hasRedisConnectionInterceptors;

    public RedisConnectionInvocationHandler(RedisConnection rawRedisConnection, ApplicationContext context,
                                            RedisConfiguration redisConfiguration,
                                            String redisTemplateBeanName, byte sourceFrom) {
        this.rawRedisConnection = rawRedisConnection;
        this.context = context;
        this.redisConfiguration = redisConfiguration;
        this.applicationName = redisConfiguration.getApplicationName();
        this.redisTemplateBeanName = redisTemplateBeanName;
        this.sourceFrom = sourceFrom;
        this.redisConnectionInterceptors = getRedisConnectionInterceptors(context);
        this.hasRedisConnectionInterceptors = !redisConnectionInterceptors.isEmpty();
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

    private List<RedisConnectionInterceptor> getRedisConnectionInterceptors(ApplicationContext context) {
        Map<String, RedisConnectionInterceptor> beansMap = beansOfTypeIncludingAncestors(context, RedisConnectionInterceptor.class, true, false);
        List<RedisConnectionInterceptor> redisConnectionInterceptors = new ArrayList<>(beansMap.values());
        AnnotationAwareOrderComparator.sort(redisConnectionInterceptors);
        return redisConnectionInterceptors;
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
        if (hasRedisConnectionInterceptors) {
            for (RedisConnectionInterceptor interceptor : redisConnectionInterceptors) {
                try {
                    interceptor.beforeExecute(redisTemplateBeanName, rawRedisConnection, method, args);
                } catch (Throwable e) {
                    logger.error("beforeExecute method fails to execute, RedisTemplate[bean name: '{}'], method: '{}'",
                            interceptor.getClass().getName(), redisTemplateBeanName, method, e);
                }
            }
        }
    }

    private void afterExecute(Method method, Object[] args, Object result, Throwable failure) {
        if (hasRedisConnectionInterceptors) {
            for (RedisConnectionInterceptor interceptor : redisConnectionInterceptors) {
                try {
                    interceptor.afterExecute(redisTemplateBeanName, rawRedisConnection, method, args, ofNullable(result), ofNullable(failure));
                } catch (Throwable e) {
                    logger.error("afterExecute method failed to execute, RedisTemplate[bean name: '{}'], method: '{}'",
                            interceptor.getClass().getName(), redisTemplateBeanName, method, e);
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
            redisCommandEvent.setBeanName(redisTemplateBeanName);
        } catch (Throwable e) {
            logger.error("Redis Sync failed to create a command method event.", method, e);
        }
        return redisCommandEvent;
    }
}
