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
package io.github.microsphere.spring.redis.annotation;

import io.github.microsphere.spring.redis.beans.RedisConnectionFactoryWrapperBeanPostProcessor;
import io.github.microsphere.spring.redis.beans.RedisTemplateWrapperBeanPostProcessor;
import io.github.microsphere.spring.redis.interceptor.EventPublishingRedisCommendInterceptor;
import io.github.microsphere.spring.redis.metadata.MethodMetadataRepository;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.util.StringUtils.commaDelimitedListToSet;
import static org.springframework.util.StringUtils.hasText;
import static org.springframework.util.StringUtils.trimWhitespace;


/**
 * Redis Interceptor {@link ImportBeanDefinitionRegistrar}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableRedisInterceptor
 * @since 1.0.0
 */
public class RedisInterceptorBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private BeanDefinitionRegistry registry;

    private ConfigurableEnvironment environment;

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        this.registry = registry;

        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableRedisInterceptor.class.getName());
        String[] wrapRedisTemplates = (String[]) attributes.get("wrapRedisTemplates");

        if (ObjectUtils.isEmpty(wrapRedisTemplates)) {
            registerRedisConnectionFactoryWrapperBeanPostProcessor();
        } else {
            registerRedisTemplateWrapperBeanPostProcessor(wrapRedisTemplates);
        }

        boolean exposeCommandEvent = (boolean) attributes.get("exposeCommandEvent");
        if (exposeCommandEvent) {
            MethodMetadataRepository.init();
            registerEventPublishingRedisCommendInterceptor();
        }
    }

    private void registerRedisTemplateWrapperBeanPostProcessor(String[] wrapRedisTemplates) {
        Set<String> wrappedRedisTemplateBeanNames = new LinkedHashSet<>();
        for (String wrapRedisTemplate : wrapRedisTemplates) {
            String wrappedRedisTemplateBeanName = environment.resolveRequiredPlaceholders(wrapRedisTemplate);
            Set<String> beanNames = commaDelimitedListToSet(wrappedRedisTemplateBeanName);
            for (String beanName : beanNames) {
                wrappedRedisTemplateBeanName = trimWhitespace(beanName);
                if (hasText(wrappedRedisTemplateBeanName)) {
                    wrappedRedisTemplateBeanNames.add(wrappedRedisTemplateBeanName);
                }
            }
        }
        if (!wrappedRedisTemplateBeanNames.isEmpty()) {
            registerBeanDefinition(RedisTemplateWrapperBeanPostProcessor.BEAN_NAME, RedisTemplateWrapperBeanPostProcessor.class, wrappedRedisTemplateBeanNames);
        }
    }

    private void registerRedisConnectionFactoryWrapperBeanPostProcessor() {
        registerBeanDefinition(RedisConnectionFactoryWrapperBeanPostProcessor.BEAN_NAME, RedisConnectionFactoryWrapperBeanPostProcessor.class);
    }

    private void registerEventPublishingRedisCommendInterceptor() {
        registerBeanDefinition(EventPublishingRedisCommendInterceptor.BEAN_NAME, EventPublishingRedisCommendInterceptor.class);
    }

    private void registerBeanDefinition(String beanName, Class<?> beanClass, Object... constructorArgs) {
        if (!registry.containsBeanDefinition(beanName)) {
            BeanDefinitionBuilder beanDefinitionBuilder = genericBeanDefinition(beanClass);
            for (Object constructorArg : constructorArgs) {
                beanDefinitionBuilder.addConstructorArgValue(constructorArg);
            }
            registry.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

}
