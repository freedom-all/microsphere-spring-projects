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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * The composite class of {@link WrapperCustomizer}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class WrapperCustomizers implements InitializingBean, BeanFactoryAware {

    public static final String BEAN_NAME = "wrapperCustomizers";

    private ConfigurableListableBeanFactory beanFactory;

    private Map<Class<?>, ObjectProvider<WrapperCustomizer>> wrapperCustomizersMap;

    public <W extends Wrapper> W customize(W wrapper) {
        Class<?> wrapperType = wrapper.getClass();
        wrapperCustomizersMap.computeIfPresent(wrapperType, (type, customizers) -> {
            customizers.forEach(c -> c.customize(wrapper));
            return customizers;
        });
        return wrapper;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory, "The 'beanFactory' is not a instance of ConfigurableListableBeanFactory");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.wrapperCustomizersMap = initWrapperCustomizersMap();
    }

    private Map<Class<?>, ObjectProvider<WrapperCustomizer>> initWrapperCustomizersMap() {
        Map<Class<?>, ObjectProvider<WrapperCustomizer>> wrapperCustomizersMap = new HashMap<>(2);
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
            ResolvableType resolvableType = beanDefinition.getResolvableType();
            Class<?> beanClass = resolvableType.getRawClass();
            if (WrapperCustomizer.class.isAssignableFrom(beanClass)) {
                Class<?> redisTemplateClass = resolvableType.getGeneric(0).getRawClass();
                wrapperCustomizersMap.computeIfAbsent(redisTemplateClass, type -> beanFactory.getBeanProvider(resolvableType));
            }
        }
        return wrapperCustomizersMap;
    }

}
