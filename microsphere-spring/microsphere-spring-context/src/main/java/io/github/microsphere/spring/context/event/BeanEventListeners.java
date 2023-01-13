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
package io.github.microsphere.spring.context.event;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * The composite {@link BeanEventListener}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
class BeanEventListeners {

    private static final String BEAN_NAME = "beanEventListeners";

    private final List<BeanEventListener> listeners;

    public BeanEventListeners(ConfigurableListableBeanFactory beanFactory) {
        this.listeners = new ArrayList<>(beanFactory.getBeansOfType(BeanEventListener.class).values());
        AnnotationAwareOrderComparator.sort(listeners);
    }

    public void onBeanDefinitionReady(String beanName, BeanDefinition beanDefinition) {
        iterate(listener -> listener.onBeanDefinitionReady(beanName, beanDefinition));
    }

    public void beforeBeanInstantiate(String beanName, Class<?> beanClass) {
        iterate(listener -> listener.beforeBeanInstantiate(beanName, beanClass));
    }

    public void beanInstantiated(String beanName, Object bean) {
        iterate(listener -> listener.beanInstantiated(beanName, bean));
    }

    public void afterBeanInstantiated(String beanName, Object bean) {
        iterate(listener -> listener.afterBeanInstantiated(beanName, bean));
    }

    public void onPropertyValuesReady(String beanName, Object bean, PropertyValues pvs) {
        iterate(listener -> listener.onPropertyValuesReady(beanName, bean, pvs));
    }

    public void beforeBeanInitialize(String beanName, Object bean) {
        iterate(listener -> listener.beforeBeanInitialize(beanName, bean));
    }

    public void afterBeanInitialized(String beanName, Object bean) {
        iterate(listener -> listener.afterBeanInitialized(beanName, bean));
    }

    public void onBeanReady(String beanName, Object bean) {
        iterate(listener -> listener.onBeanReady(beanName, bean));
    }

    public void beforeBeanDestroy(String beanName, Object bean) {
        iterate(listener -> listener.beforeBeanDestroy(beanName, bean));
    }

    public void afterBeanDestroy(String beanName, Object bean) {
        iterate(listener -> listener.afterBeanDestroy(beanName, bean));
    }

    private void iterate(Consumer<BeanEventListener> listenerConsumer) {
        listeners.forEach(listenerConsumer);
    }

    public void registerBean(BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder beanDefinitionBuilder = rootBeanDefinition(BeanEventListeners.class, () -> this);
        beanDefinitionBuilder.setPrimary(true);
        registry.registerBeanDefinition(BEAN_NAME, beanDefinitionBuilder.getBeanDefinition());
    }

    public static BeanEventListeners getBean(BeanFactory beanFactory) {
        return beanFactory.getBean(BEAN_NAME, BeanEventListeners.class);
    }
}
