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

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.InstantiationStrategy;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Bean Before-Event Publishing Processor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class BeanBeforeEventPublishingProcessor extends InstantiationAwareBeanPostProcessorAdapter implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, BeanDefinitionRegistryPostProcessor,
        InstantiationStrategy, PropertyEditorRegistrar, PriorityOrdered {

    private static final Function<BeanFactory, InstantiationStrategy> instantiationStrategyResolver = beanFactory -> {
        InstantiationStrategy instantiationStrategy = null;
        try {
            Method method = AbstractAutowireCapableBeanFactory.class.getDeclaredMethod("getInstantiationStrategy");
            method.setAccessible(true);
            instantiationStrategy = (InstantiationStrategy) method.invoke(beanFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return instantiationStrategy;
    };

    private ConfigurableApplicationContext context;

    private BeanDefinitionRegistry registry;

    private ConfigurableListableBeanFactory beanFactory;

    private InstantiationStrategy instantiationStrategyDelegate;

    private BeanEventListeners beanEventListeners;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        this.context = context;
        // Add Self
        context.addBeanFactoryPostProcessor(this);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        registerBeanEventListeners(registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        decorateInstantiationStrategy(beanFactory);
        beanFactory.addBeanPostProcessor(this);
        beanFactory.addPropertyEditorRegistrar(this);
    }

    private void registerBeanEventListeners(BeanDefinitionRegistry registry) {
        BeanEventListeners beanEventListeners = new BeanEventListeners(context);
        BeanDefinitionBuilder beanDefinitionBuilder = rootBeanDefinition(BeanEventListeners.class, () -> beanEventListeners);
        String beanName = "beanEventListeners";
        registry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        this.beanEventListeners = beanEventListeners;
    }

    private void decorateInstantiationStrategy(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory instanceof AbstractAutowireCapableBeanFactory) {
            this.instantiationStrategyDelegate = getInstantiationStrategyDelegate(beanFactory);
            if (instantiationStrategyDelegate != this) {
                AbstractAutowireCapableBeanFactory autowireCapableBeanFactory = (AbstractAutowireCapableBeanFactory) beanFactory;
                autowireCapableBeanFactory.setInstantiationStrategy(this);
            }
        }
    }

    private InstantiationStrategy getInstantiationStrategyDelegate(ConfigurableListableBeanFactory beanFactory) {
        return instantiationStrategyResolver.apply(beanFactory);
    }

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (!BeanFactoryPostProcessor.class.isAssignableFrom(beanClass)
                && !BeanPostProcessor.class.isAssignableFrom(beanClass)
        ) {
            this.beanEventListeners.onBeforeInstantiation(beanClass, beanName);
        }
        return null;
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner) throws BeansException {
        Object bean = instantiationStrategyDelegate.instantiate(bd, beanName, owner);
        this.beanEventListeners.onInstantiating(bean, beanName);
        return bean;
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner, Constructor<?> ctor, Object... args) throws BeansException {
        Object bean = instantiationStrategyDelegate.instantiate(bd, beanName, owner, ctor, args);
        this.beanEventListeners.onInstantiating(bean, beanName);
        return bean;
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner, Object factoryBean, Method factoryMethod, Object... args) throws BeansException {
        Object bean = instantiationStrategyDelegate.instantiate(bd, beanName, owner, factoryBean, factoryMethod, args);
        this.beanEventListeners.onInstantiating(bean, beanName);
        return bean;
    }

    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        if (registry instanceof BeanWrapper) {
            BeanWrapper beanWrapper = (BeanWrapper) registry;
            Class<?> beanClass = beanWrapper.getWrappedClass();
            Object bean = beanWrapper.getWrappedInstance();
        }
    }

    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        this.beanEventListeners.onPropertyValuesReady(pvs, bean, beanName);
        return null;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        this.beanEventListeners.onBeforeInitialization(bean, beanName);
        return bean;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

}
