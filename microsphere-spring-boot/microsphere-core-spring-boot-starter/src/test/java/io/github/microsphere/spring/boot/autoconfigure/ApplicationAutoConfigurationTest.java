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
package io.github.microsphere.spring.boot.autoconfigure;

import io.github.microsphere.spring.context.event.BeanEventListener;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Application AutoConfiguration Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@SpringBootTest(classes = {
        ApplicationAutoConfigurationTest.class,
        ApplicationAutoConfigurationTest.LoggingBeanEventListener.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
public class ApplicationAutoConfigurationTest {

    @Test
    public void test() {

    }

    static class LoggingBeanEventListener implements BeanEventListener {

        private static final Logger logger = LoggerFactory.getLogger(LoggingBeanEventListener.class);

        @Override
        public void onBeanDefinitionReady(String beanName, BeanDefinition beanDefinition) {
            logger.info("onBeanDefinitionReady - bean name : {} , definition : {}", beanName, beanDefinition);
        }

        @Override
        public void onBeforeInstantiation(String beanName, Class<?> beanClass) {
            logger.info("onBeforeInstantiation - bean name : {} , class : {}", beanName, beanClass);
        }

        @Override
        public void onInstantiating(String beanName, Object bean) {
            logger.info("onInstantiating - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onInstantiated(String beanName, Object bean) {
            logger.info("onInstantiated - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onPropertyValuesReady(String beanName, Object bean, PropertyValues pvs) {
            logger.info("onPropertyValuesReady - bean name : {} , instance : {} , PropertyValues : {}", beanName, bean, pvs);
        }

        @Override
        public void onBeforeInitialization(String beanName, Object bean) {
            logger.info("onBeforeInitialization - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onInitialized(String beanName, Object bean) {
            logger.info("onInitialized - bean name : {} , instance : {}", beanName, bean);
        }

        @Override
        public void onBeforeDestruction(String beanName, Object bean) {
            logger.info("onPreDestroy - bean name : {} , instance : {}", beanName, bean);
        }
    }
}
