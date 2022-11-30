/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microsphere.spring.redis.interceptor;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.context.RedisContext;
import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.metadata.ParameterMetadata;
import io.github.microsphere.spring.redis.serializer.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import static io.github.microsphere.spring.redis.metadata.MethodMetadataRepository.getWriteParameterMetadataList;
import static io.github.microsphere.spring.redis.metadata.MethodMetadataRepository.isWriteCommandMethod;
import static io.github.microsphere.spring.redis.util.RedisCommandsUtils.buildParameterMetadataList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Redis Method Context
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class RedisMethodContext<T> {

    private static final Logger logger = LoggerFactory.getLogger(RedisMethodContext.class);

    private static final Parameter[] EMPTY_PARAMETERS = new Parameter[0];

    private static final ThreadLocal<RedisMethodContext<?>> redisMethodContextThreadLocal = new ThreadLocal<>();

    private final T target;

    private final Method method;

    private final Object[] args;

    private Parameter[] parameters = null;

    private Map<Object, Parameter> parametersMap = null;

    private Integer parameterCount = null;

    private Boolean write = null;

    private final RedisContext redisContext;

    private final String sourceBeanName;

    private Boolean sourceFromRedisTemplate = null;

    private Boolean sourceFromRedisConnectionFactory = null;

    private long startTimeNanos = -1;

    private long durationNanos = -1;

    public RedisMethodContext(T target, Method method, Object[] args, RedisContext redisContext) {
        this(target, method, args, redisContext, null);
    }

    public RedisMethodContext(T target, Method method, Object[] args, RedisContext redisContext, String sourceBeanName) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.redisContext = redisContext;
        this.sourceBeanName = sourceBeanName;
    }

    public T getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getSourceBeanName() {
        return sourceBeanName;
    }

    public RedisContext getRedisContext() {
        return redisContext;
    }

    private void initParameters() {
        List<ParameterMetadata> parameterMetadataList = getWriteParameterMetadataList(method);
        if (parameterMetadataList != null) {
            initParameters(parameterMetadataList, true);
        } else {
            parameterMetadataList = buildParameterMetadataList(method);
            initParameters(parameterMetadataList, false);
        }
    }

    private void initParameters(List<ParameterMetadata> parameterMetadataList, boolean write) {
        final Parameter[] parameters;
        final Map<Object, Parameter> parametersMap;
        final int size = parameterMetadataList == null ? 0 : parameterMetadataList.size();

        if (size > 0) {
            parameters = new Parameter[size];
            parametersMap = new HashMap<>(size);
            try {
                for (int i = 0; i < size; i++) {
                    Object parameterValue = args[i];
                    ParameterMetadata parameterMetadata = parameterMetadataList.get(i);
                    Parameter parameter = new Parameter(parameterValue, parameterMetadata);
                    // serialize parameter
                    Serializers.serializeRawParameter(parameter);
                    parameters[i] = parameter;
                    parametersMap.put(parameterValue, parameter);
                }
            } catch (Throwable e) {
                logger.error("Redis failed to initialize Redis command method parameter {}!", parameterMetadataList, e);
            }
        } else {
            parameters = EMPTY_PARAMETERS;
            parametersMap = emptyMap();
        }

        this.parameters = parameters;
        this.parametersMap = unmodifiableMap(parametersMap);
        this.parameterCount = size;
        this.write = write;
    }

    /**
     * Start and record the time in nano seconds, the initialized value is negative
     */
    public void start() {
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Stop and record the time in nano seconds, the initialized value is negative
     *
     * @throws IllegalStateException if {@link #start()} is not execute before
     */
    public void stop() {
        if (startTimeNanos < 0) {
            throw new IllegalStateException("'stop()' method must not be invoked before the execution of 'start()' method");
        }
        this.durationNanos = System.nanoTime() - startTimeNanos;
    }

    /**
     * Get the start time in nano seconds
     *
     * @return If the value is negative, it indicates {@link #start()} method was not executed
     */
    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    /**
     * Get the execution duration time of redis method in nano seconds
     *
     * @return If the value is negative, it indicates the duration can't not be evaluated,
     * because {@link #start()} method was not executed
     */
    public long getDurationNanos() {
        return durationNanos;
    }

    /**
     * Get the execution duration time of redis method in the specified {@link TimeUnit time unit}
     *
     * @return If the value is negative, it indicates the duration can't not be evaluated,
     * because {@link #start()} method was not executed
     */
    public long getDuration(TimeUnit timeUnit) {
        long durationNanos = getDurationNanos();
        if (durationNanos < 0) {
            return durationNanos;
        }
        return timeUnit.convert(durationNanos, TimeUnit.NANOSECONDS);
    }

    public boolean isWriteMethod() {
        if (write == null) {
            return isWriteMethod(false);
        }
        return write;
    }

    public boolean isWriteMethod(boolean initializedParameters) {
        if (initializedParameters) {
            initParameters();
        } else {
            this.write = isWriteCommandMethod(method);
        }
        return this.write;
    }

    public Map<Object, Parameter> getParametersMap() {
        if (parametersMap == null) {
            initParameters();
        }
        return parametersMap;
    }

    public Parameter[] getParameters() {
        if (parameters == null) {
            initParameters();
        }
        return parameters;
    }

    public int getParameterCount() {
        if (parameterCount == null) {
            initParameters();
        }
        return parameterCount.intValue();
    }

    public Parameter getParameterMap(Object parameterValue) {
        return getParametersMap().get(parameterValue);
    }

    public Parameter getParameter(int index) {
        return getParameters()[index];
    }

    @NonNull
    public RedisConfiguration getRedisConfiguration() {
        return redisContext.getRedisConfiguration();
    }

    public ConfigurableListableBeanFactory getBeanFactory() {
        return redisContext.getBeanFactory();
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return redisContext.getApplicationContext();
    }

    public ClassLoader getClassLoader() {
        return redisContext.getClassLoader();
    }

    public Set<String> getRedisTemplateBeanNames() {
        return redisContext.getRedisTemplateBeanNames();
    }

    public Set<String> getRedisConnectionFactoryBeanNames() {
        return redisContext.getRedisConnectionFactoryBeanNames();
    }

    public boolean isEnabled() {
        return redisContext.isEnabled();
    }

    public ConfigurableEnvironment getEnvironment() {
        return redisContext.getEnvironment();
    }

    public boolean isCommandEventExposed() {
        return redisContext.isCommandEventExposed();
    }

    public String getApplicationName() {
        return redisContext.getApplicationName();
    }

    public boolean isSourceFromRedisTemplate() {
        Boolean sourceFromRedisTemplate = this.sourceFromRedisTemplate;
        if (sourceFromRedisTemplate == null) {
            sourceFromRedisTemplate = redisContext.getRedisTemplateBeanNames().contains(sourceBeanName);
            this.sourceFromRedisTemplate = sourceFromRedisTemplate;
        }
        return sourceFromRedisTemplate;
    }

    public boolean isSourceFromRedisConnectionFactory() {
        Boolean sourceFromRedisConnectionFactory = this.sourceFromRedisConnectionFactory;
        if (sourceFromRedisConnectionFactory == null) {
            sourceFromRedisConnectionFactory = redisContext.getRedisConnectionFactoryBeanNames().contains(sourceBeanName);
            this.sourceFromRedisConnectionFactory = sourceFromRedisConnectionFactory;
        }
        return sourceFromRedisConnectionFactory;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RedisMethodContext.class.getSimpleName() + "[", "]").add("target=" + target).add("method=" + method).add("args=" + Arrays.toString(args)).add("write=" + write).add("parameters=" + parameters).add("redisContext=" + redisContext).add("sourceBeanName='" + sourceBeanName + "'").add("startTimeNanos=" + startTimeNanos).add("durationNanos=" + durationNanos).toString();
    }

    public static void set(RedisMethodContext redisMethodContext) {
        redisMethodContextThreadLocal.set(redisMethodContext);
        logger.debug("{} stores into ThreadLocal", redisMethodContext);
    }

    public static <T> RedisMethodContext<T> get() {
        return (RedisMethodContext<T>) redisMethodContextThreadLocal.get();
    }

    public static void clear() {
        redisMethodContextThreadLocal.remove();
    }
}
