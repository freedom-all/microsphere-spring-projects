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
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * {@link BeanEventListener} Adapter
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class BeanEventListenerAdapter implements BeanEventListener {

    @Override
    public void beanDefinitionReady(String beanName, BeanDefinition beanDefinition) {
    }

    @Override
    public void beforeInstantiate(String beanName, Class<?> beanClass) {
    }

    @Override
    public void instantiated(String beanName, Object bean) {
    }

    @Override
    public void afterInstantiated(String beanName, Object bean) {
    }

    @Override
    public void propertyValuesReady(String beanName, Object bean, PropertyValues pvs) {
    }

    @Override
    public void beforeInitialize(String beanName, Object bean) {
    }

    @Override
    public void afterInitialized(String beanName, Object bean) {
    }

    @Override
    public void beforeDestroy(String beanName, Object bean) {
    }
}
