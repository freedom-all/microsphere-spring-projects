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

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link BeanEventListener} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        BeanEventPublishingInitializerTest.class,
        BeanEventPublishingInitializerTest.LoggingBeanEventListener.class
},
        initializers = {
                BeanEventPublishingInitializer.class
        })
public class BeanEventPublishingInitializerTest {

    @org.junit.Test
    public void test() {

    }

    static class LoggingBeanEventListener implements BeanEventListener {

        private static final Logger logger = LoggerFactory.getLogger(LoggingBeanEventListener.class);

        @Override
        public void onBeanDefinitionReady(String beanName, BeanDefinition beanDefinition) {
            logger.info("onBeanDefinitionReady - bean name : {} , definition : {}", beanName, beanDefinition);
        }

        @Override
        public void onBeforeBeanInstantiate(String beanName, Class<?> beanClass) {
            logger.info("onBeforeBeanInstantiate - bean name : {} , class : {}", beanName, beanClass);
        }

        @Override
        public void onBeanInstantiating(String beanName, Object bean) {
            logger.info("onBeanInstantiating - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onAfterBeanInstantiated(String beanName, Object bean) {
            logger.info("onAfterBeanInstantiated - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onBeanPropertyValuesReady(String beanName, Object bean, PropertyValues pvs) {
            logger.info("onBeanPropertyValuesReady - bean name : {} , instance : {} , PropertyValues : {}", beanName, bean, pvs);
        }

        @Override
        public void onBeforeBeanInitialize(String beanName, Object bean) {
            logger.info("onBeforeBeanInitialize - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onAfterBeanInitialized(String beanName, Object bean) {
            logger.info("onAfterBeanInitialized - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onBeanReady(String beanName, Object bean) {
            logger.info("onBeanReady - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onBeforeBeanDestroy(String beanName, Object bean) {
            logger.info("onBeforeBeanDestroy - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onAfterBeanDestroy(String beanName, Object bean) {
            logger.info("onAfterBeanDestroy - bean name : {} , instance : {}", beanName, bean);
        }
    }
}
