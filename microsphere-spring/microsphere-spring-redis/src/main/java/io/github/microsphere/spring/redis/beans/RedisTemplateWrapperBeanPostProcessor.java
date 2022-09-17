package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.REDIS_TEMPLATE_BEAN_NAME;
import static io.github.microsphere.spring.redis.config.RedisConfiguration.STRING_REDIS_TEMPLATE_BEAN_NAME;

/**
 * {@link BeanPostProcessor} 实现 Wrapper {@link RedisTemplate} 和 {@link StringRedisTemplate}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RedisTemplateWrapper
 * @see StringRedisTemplateWrapper
 * @see BeanPostProcessor
 * @since 1.0.0
 */
public class RedisTemplateWrapperBeanPostProcessor implements BeanPostProcessor {

    private final ApplicationContext context;

    private final RedisConfiguration redisConfiguration;

    private final Set<String> redisTemplateBeanNames;

    public RedisTemplateWrapperBeanPostProcessor(ApplicationContext context, RedisConfiguration redisConfiguration) {
        this.context = context;
        this.redisConfiguration = redisConfiguration;
        this.redisTemplateBeanNames = initRedisTemplateBeanNames(context);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (redisTemplateBeanNames.contains(beanName)) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            if (StringRedisTemplate.class.equals(beanClass)) {
                StringRedisTemplateWrapper stringRedisTemplateWrapper = new StringRedisTemplateWrapper((StringRedisTemplate) bean, context);
                stringRedisTemplateWrapper.setBeanName(beanName);
                return stringRedisTemplateWrapper;
            } else if (RedisTemplate.class.equals(beanClass)) {
                RedisTemplateWrapper redisTemplateWrapper = new RedisTemplateWrapper((RedisTemplate) bean, context);
                redisTemplateWrapper.setBeanName(beanName);
                return redisTemplateWrapper;
            }
            // TODO 支持更多的自定义 RedisTemplate 类型
        }
        return bean;
    }

    private Set<String> initRedisTemplateBeanNames(ApplicationContext context) {
        Environment environment = context.getEnvironment();

        List<String> additionalRedisTemplateBeanNames = redisConfiguration.getAdditionalRedisTemplateBeanNames(environment);
        Set<String> redisTemplateBeanNames = new HashSet<>(additionalRedisTemplateBeanNames.size() + 2);
        // 添加默认 RedisTemplate Bean 名称
        // 匹配 RedisAutoConfiguration 或 RedisServiceAutoConfiguration 中的 RedisTemplate 以及 StringRedisTemplate
        redisTemplateBeanNames.add(REDIS_TEMPLATE_BEAN_NAME);
        redisTemplateBeanNames.add(STRING_REDIS_TEMPLATE_BEAN_NAME);
        // 应用自定义配置
        redisTemplateBeanNames.addAll(additionalRedisTemplateBeanNames);
        return Collections.unmodifiableSet(redisTemplateBeanNames);

    }
}
