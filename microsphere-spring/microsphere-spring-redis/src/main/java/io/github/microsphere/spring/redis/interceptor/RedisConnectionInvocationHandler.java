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

        if (parameterMetadataList != null) { // 当前方法为复制 Redis 命令方法
            // 初始化方法参数数据
            ParametersHolder.init(parameterMetadataList, args);
            // 执行Redis 命令方法方法
            result = doInvoke(method, args);
            // 发布 Redis 命令方法事件
            publishRedisCommandEvent(method, args);
        } else {
            // 执行 Redis 命令方法方法
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
                    logger.error("{}.beforeExecute 方法执行失败 , RedisTemplate[bean name : '{}'] , method : '{}'",
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
                    logger.error("{}.afterExecute 方法执行失败 , RedisTemplate[bean name : '{}'] , method : '{}'",
                            interceptor.getClass().getName(), redisTemplateBeanName, method, e);
                }
            }
        }
    }

    private void publishRedisCommandEvent(Method method, Object[] args) {
        RedisCommandEvent redisCommandEvent = createRedisCommandEvent(method, args);
        if (redisCommandEvent != null) {
            // 事件处理允许抛出异常
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
            logger.error("Redis Sync 创建命令方法事件失败，方法：{}", method, e);
        }
        return redisCommandEvent;
    }
}
