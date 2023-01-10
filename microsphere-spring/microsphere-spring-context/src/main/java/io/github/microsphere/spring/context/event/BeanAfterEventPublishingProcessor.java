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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Bean After-Event Publishing Processor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class BeanAfterEventPublishingProcessor extends InstantiationAwareBeanPostProcessorAdapter implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, BeanFactoryPostProcessor {

    private static final String BEAN_NAME = "beanAfterEventPublishingProcessor";
    private static final Class<BeanAfterEventPublishingProcessor> BEAN_CLASS = BeanAfterEventPublishingProcessor.class;

    private BeanEventListeners beanEventListeners;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        registerSelfAsBean(context);
    }

    private void registerSelfAsBean(ConfigurableApplicationContext context) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            BeanDefinitionBuilder beanDefinitionBuilder = rootBeanDefinition(BEAN_CLASS, () -> this);
            registry.registerBeanDefinition(BEAN_NAME, beanDefinitionBuilder.getBeanDefinition());
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanEventListeners = getBeanEventListeners(beanFactory);
        beanFactory.addBeanPostProcessor(this);
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            this.beanEventListeners.onBeanDefinitionReady(beanDefinition, beanName);
        }
    }

    private BeanEventListeners getBeanEventListeners(ConfigurableListableBeanFactory beanFactory) {
        return beanFactory.getBean(BeanEventListeners.class);
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        this.beanEventListeners.onBeanInstantiated(bean, beanName);
        return true;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        this.beanEventListeners.onInitialized(bean, beanName);
        return bean;
    }
}
