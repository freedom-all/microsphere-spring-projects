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

import java.util.EventListener;

/**
 * Bean {@link EventListener}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface BeanEventListener extends EventListener {

    void onBeanDefinitionReady(BeanDefinition beanDefinition, String beanName);

    void onBeforeInstantiation(Class<?> beanClass, String beanName);

    void onInstantiating(Object bean, String beanName);

    void onInstantiated(Object bean, String beanName);

    void onPropertyValuesReady(PropertyValues pvs, Object bean, String beanName);

    void onBeforeInitialization(Object bean, String beanName);

    void onInitialized(Object bean, String beanName);

}
