package io.github.microsphere.spring.redis.beans;

import io.github.microsphere.spring.redis.config.RedisConfiguration;
import io.github.microsphere.spring.util.BeanUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static io.github.microsphere.spring.redis.config.RedisConfiguration.PROPERTY_NAME_PREFIX;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * {@link BeanPostProcessor} implements Wrapper {@link RedisTemplate} and {@link StringRedisTemplate}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RedisTemplateWrapper
 * @see StringRedisTemplateWrapper
 * @see BeanPostProcessor
 * @since 1.0.0
 */
public class RedisTemplateWrapperBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, EnvironmentAware {

    public static final String BEAN_NAME = "redisTemplateWrapperBeanPostProcessor";

    /**
     * {@link RedisTemplate} Bean Name
     */
    public static final String REDIS_TEMPLATE_BEAN_NAME = "redisTemplate";

    /**
     * {@link StringRedisTemplate} Bean Name
     */
    public static final String STRING_REDIS_TEMPLATE_BEAN_NAME = "stringRedisTemplate";

    /**
     * Wrapped {@link RedisTemplate} list of Bean names
     */
    public static final String WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME = PROPERTY_NAME_PREFIX + "wrapped-redis-templates";

    /**
     * The default wrapped bean names of {@link RedisTemplate}:
     * <ul>
     *     <li>{@link #REDIS_TEMPLATE_BEAN_NAME "redisTemplate"}</li>
     *     <li>{@link #STRING_REDIS_TEMPLATE_BEAN_NAME "stringRedisTemplate"}</li>
     * </ul>
     *
     * @see #REDIS_TEMPLATE_BEAN_NAME
     * @see #STRING_REDIS_TEMPLATE_BEAN_NAME
     */
    public static final List<String> DEFAULT_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES = unmodifiableList(asList(REDIS_TEMPLATE_BEAN_NAME, STRING_REDIS_TEMPLATE_BEAN_NAME));

    /**
     * The all wrapped bean names of {@link RedisTemplate}: "*"
     */
    public static final List<String> ALL_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES = unmodifiableList(asList("*"));

    private ConfigurableListableBeanFactory beanFactory;

    private final RedisConfiguration redisConfiguration;

    private List<String> wrappedRedisTemplateBeanNames;

    public RedisTemplateWrapperBeanPostProcessor(RedisConfiguration redisConfiguration) {
        this.redisConfiguration = redisConfiguration;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (wrappedRedisTemplateBeanNames.contains(beanName)) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);
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
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.wrappedRedisTemplateBeanNames = resolveWrappedRedisTemplateBeanNames(environment);
    }

    /**
     * Resolve the wrapped {@link RedisTemplate} Bean Name list, the default value is from {@link #DEFAULT_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES}
     *
     * @param environment {@link Environment}
     * @return If no configuration is found, {@link #DEFAULT_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES} is returned
     */
    private List<String> resolveWrappedRedisTemplateBeanNames(Environment environment) {
        List<String> wrappedRedisTemplateBeanNames = environment.getProperty(WRAPPED_REDIS_TEMPLATE_BEAN_NAMES_PROPERTY_NAME, List.class);
        if (wrappedRedisTemplateBeanNames == null) {
            return DEFAULT_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES;
        } else if (ALL_WRAPPED_REDIS_TEMPLATE_BEAN_NAMES.equals(wrappedRedisTemplateBeanNames)) {
            return resolveAllRestTemplateBeanNames();
        }else {
            return unmodifiableList(wrappedRedisTemplateBeanNames);
        }
    }

    private List<String> resolveAllRestTemplateBeanNames() {
        String[] redisTemplateBeanNames = BeanUtils.getBeanNames(beanFactory, RedisTemplate.class);
        return unmodifiableList(asList(redisTemplateBeanNames));
    }
}
