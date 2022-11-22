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
package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.redis.connection.RedisConnectionFactoryWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.springframework.aop.framework.AopProxyUtils.ultimateTargetClass;

/**
 * {@link RedisConnectionFactoryWrapper} {@link BeanPostProcessor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see RedisConnectionFactoryWrapper
 * @since 1.0.0
 */
public class RedisConnectionFactoryWrapperBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    public static final String BEAN_NAME = "redisConnectionFactoryWrapperBeanPostProcessor";

    private RedisConfiguration redisConfiguration;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Class<?> beanClass = ultimateTargetClass(bean);

        if (RedisConnectionFactory.class.isAssignableFrom(beanClass)) {
            RedisConnectionFactory redisConnectionFactory = (RedisConnectionFactory) bean;
            return new RedisConnectionFactoryWrapper(beanName, redisConnectionFactory, redisConfiguration);
        }

        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.redisConfiguration = RedisConfiguration.get(beanFactory);
    }
}
