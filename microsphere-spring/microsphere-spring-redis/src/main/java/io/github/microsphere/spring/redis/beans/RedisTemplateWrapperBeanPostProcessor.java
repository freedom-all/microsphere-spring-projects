package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.util.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.get;
import static io.github.microsphere.spring.redis.util.RedisConstants.ALL_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES;
import static io.github.microsphere.spring.redis.util.RedisConstants.WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.springframework.aop.framework.AopProxyUtils.ultimateTargetClass;

/**
 * {@link BeanPostProcessor} implements Wrapper {@link RedisTemplate} and {@link StringRedisTemplate}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RedisTemplateWrapper
 * @see StringRedisTemplateWrapper
 * @see BeanPostProcessor
 * @since 1.0.0
 */
public class RedisTemplateWrapperBeanPostProcessor implements BeanPostProcessor, InitializingBean, ApplicationContextAware {

    public static final String BEAN_NAME = "redisTemplateWrapperBeanPostProcessor";

    private ConfigurableApplicationContext context;

    private RedisConfiguration redisConfiguration;

    private List<String> wrappedRedisTemplateBeanNames;

    public RedisTemplateWrapperBeanPostProcessor() {
    }

    public RedisTemplateWrapperBeanPostProcessor(Collection<String> wrappedRedisTemplateBeanNames) {
        this.wrappedRedisTemplateBeanNames = new ArrayList<>(wrappedRedisTemplateBeanNames);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (wrappedRedisTemplateBeanNames.contains(beanName)) {
            Class<?> beanClass = ultimateTargetClass(bean);
            if (StringRedisTemplate.class.equals(beanClass)) {
                return new StringRedisTemplateWrapper(beanName, (StringRedisTemplate) bean, redisConfiguration);
            } else if (RedisTemplate.class.equals(beanClass)) {
                return new RedisTemplateWrapper(beanName, (RedisTemplate) bean, redisConfiguration);
            }
            // TODO Support for more custom RedisTemplate types
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Assert.isInstanceOf(ConfigurableApplicationContext.class, context, "The 'context' argument must be an instance of ConfigurableApplicationContext");
        this.context = (ConfigurableApplicationContext) context;
    }

    /**
     * Resolve the wrapped {@link RedisTemplate} Bean Name list, the default value is from {@link Collections#emptyList()}
     *
     * @param context {@link ConfigurableApplicationContext}
     * @return If no configuration is found, {@link Collections#emptyList()} is returned
     */
    public static List<String> resolveWrappedRedisTemplateBeanNames(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        List<String> wrappedRedisTemplateBeanNames = environment.getProperty(WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME, List.class, emptyList());
        if (ALL_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES.equals(wrappedRedisTemplateBeanNames)) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            return resolveAllRestTemplateBeanNames(beanFactory);
        }
        return unmodifiableList(wrappedRedisTemplateBeanNames);
    }

    private static List<String> resolveAllRestTemplateBeanNames(ConfigurableListableBeanFactory beanFactory) {
        String[] redisTemplateBeanNames = BeanUtils.getBeanNames(beanFactory, RedisTemplate.class);
        return unmodifiableList(asList(redisTemplateBeanNames));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.redisConfiguration = get(context);
        if (this.wrappedRedisTemplateBeanNames == null) {
            this.wrappedRedisTemplateBeanNames = resolveWrappedRedisTemplateBeanNames(context);
        }
    }
}
