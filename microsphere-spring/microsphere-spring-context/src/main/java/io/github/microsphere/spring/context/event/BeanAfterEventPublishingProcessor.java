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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

/**
 * Bean After-Event Publishing Processor
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
class BeanAfterEventPublishingProcessor extends InstantiationAwareBeanPostProcessorAdapter {

    /**
     * {@link BeanBeforeEventPublishingProcessor} Initializer that
     * is not a general propose Spring Bean initializes {@link BeanBeforeEventPublishingProcessor}
     */
    static class Initializer {

        public Initializer(ConfigurableListableBeanFactory beanFactory) {
            beanFactory.addBeanPostProcessor(new BeanAfterEventPublishingProcessor(beanFactory));
            fireBeanDefinitionReady(beanFactory);
        }

        private void fireBeanDefinitionReady(ConfigurableListableBeanFactory beanFactory) {
            BeanEventListeners beanEventListeners = BeanEventListeners.getBean(beanFactory);
            String[] beanNames = beanFactory.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                beanEventListeners.onBeanDefinitionReady(beanDefinition, beanName);
            }
        }
    }

    private final BeanEventListeners beanEventListeners;

    public BeanAfterEventPublishingProcessor(ConfigurableBeanFactory beanFactory) {
        this.beanEventListeners = BeanEventListeners.getBean(beanFactory);
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
