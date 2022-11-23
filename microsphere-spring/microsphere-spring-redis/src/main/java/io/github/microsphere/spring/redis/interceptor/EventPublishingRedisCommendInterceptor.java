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
import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import io.github.microsphere.spring.redis.event.RedisConfigurationPropertyChangedEvent;
import io.github.microsphere.spring.redis.metadata.Parameter;
import io.github.microsphere.spring.redis.metadata.ParameterMetadata;
import io.github.microsphere.spring.redis.metadata.ParametersHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisCommands;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static io.github.microsphere.spring.redis.metadata.MethodMetadataRepository.getParameterMetadataList;
import static io.github.microsphere.spring.redis.util.RedisConstants.COMMAND_EVENT_EXPOSED_PROPERTY_NAME;

/**
 * {@link RedisCommandInterceptor} publishes {@link RedisCommandEvent}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class EventPublishingRedisCommendInterceptor implements RedisCommandInterceptor, ApplicationListener<RedisConfigurationPropertyChangedEvent>, ApplicationEventPublisherAware {
    private static final Logger logger = LoggerFactory.getLogger(EventPublishingRedisCommendInterceptor.class);

    public static final String BEAN_NAME = "eventPublishingRedisCommendInterceptor";

    private final RedisConfiguration redisConfiguration;

    private final String applicationName;

    private ApplicationEventPublisher applicationEventPublisher;

    private volatile boolean enabled = false;

    public EventPublishingRedisCommendInterceptor(RedisConfiguration redisConfiguration) {
        this.redisConfiguration = redisConfiguration;
        this.applicationName = redisConfiguration.getApplicationName();
        setEnabled();
    }

    public void setEnabled() {
        this.enabled = redisConfiguration.isCommandEventExposed();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void afterExecute(RedisCommands redisCommands, Method method, Object[] args, Optional<Object> result, Optional<Throwable> failure, Optional<String> redisTemplateBeanName) throws Throwable {
        if (isEnabled() && !failure.isPresent()) {
            List<ParameterMetadata> parameterMetadataList = getParameterMetadataList(method);
            if (parameterMetadataList != null) { // The current method is to copy the Redis command
                // Initializes the method parameter data
                ParametersHolder.init(parameterMetadataList, args);
                // Publish Redis Command Event
                publishRedisCommandEvent(method, args, redisTemplateBeanName);
            }
        }
    }

    private void publishRedisCommandEvent(Method method, Object[] args, Optional<String> redisTemplateBeanName) {
        RedisCommandEvent redisCommandEvent = createRedisCommandEvent(method, args, redisTemplateBeanName);
        if (redisCommandEvent != null) {
            // Event handling allows exceptions to be thrown
            applicationEventPublisher.publishEvent(redisCommandEvent);
        }
    }

    private RedisCommandEvent createRedisCommandEvent(Method method, Object[] args, Optional<String> redisTemplateBeanName) {
        RedisCommandEvent redisCommandEvent = null;
        try {
            Parameter[] parameters = ParametersHolder.bulkGet(args);
            redisCommandEvent = new RedisCommandEvent(method, parameters, applicationName);
            redisTemplateBeanName.ifPresent(redisCommandEvent::setSourceBeanName);
        } catch (Throwable e) {
            logger.error("Redis failed to create a command method event.", method, e);
        }
        return redisCommandEvent;
    }

    @Override
    public void onApplicationEvent(RedisConfigurationPropertyChangedEvent event) {
        if (event.hasProperty(COMMAND_EVENT_EXPOSED_PROPERTY_NAME)) {
            this.setEnabled();
        }
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
}
