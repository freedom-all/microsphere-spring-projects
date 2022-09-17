package io.github.microsphere.spring.redis.dynamic.connection;

import io.github.microsphere.commons.text.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

/**
 * 动态 {@link RedisConnectionFactory} 实现
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class DynamicRedisConnectionFactory implements RedisConnectionFactory, SmartInitializingSingleton,
        ApplicationContextAware, BeanNameAware {

    /**
     * 默认 {@link RedisConnectionFactory} Bean 名称
     */
    public static final String DEFAULT_REDIS_CONNECTION_FACTORY_BEAN_NAME = "redisConnectionFactory";

    private static final Logger logger = LoggerFactory.getLogger(DynamicRedisConnectionFactory.class);

    private static final ThreadLocal<String> beanNameHolder = new ThreadLocal<>();

    private String beanName;

    private ApplicationContext context;

    private Map<String, RedisConnectionFactory> redisConnectionFactories;

    private String defaultRedisConnectionFactoryBeanName = DEFAULT_REDIS_CONNECTION_FACTORY_BEAN_NAME;

    private RedisConnectionFactory defaultRedisConnectionFactory;

    @Override
    public RedisConnection getConnection() {
        return determineTargetRedisConnectionFactory().getConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return determineTargetRedisConnectionFactory().getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return determineTargetRedisConnectionFactory().getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return determineTargetRedisConnectionFactory().getSentinelConnection();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return determineTargetRedisConnectionFactory().translateExceptionIfPossible(ex);
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initialize();
    }

    public void setDefaultRedisConnectionFactoryBeanName(String defaultRedisConnectionFactoryBeanName) {
        this.defaultRedisConnectionFactoryBeanName = defaultRedisConnectionFactoryBeanName;
    }

    public String getDefaultRedisConnectionFactoryBeanName() {
        return defaultRedisConnectionFactoryBeanName;
    }

    protected void initialize() {
        this.redisConnectionFactories = resolveRedisConnectionFactories();
        this.defaultRedisConnectionFactory = resolveDefaultRedisConnectionFactory();
    }

    @NonNull
    protected RedisConnectionFactory determineTargetRedisConnectionFactory() {
        String targetBeanName = getTargetBeanName();
        if (targetBeanName == null) {
            RedisConnectionFactory defaultRedisConnectionFactory = getDefaultRedisConnectionFactory();
            logger.debug("当前目标 Bean Name 未设置或在多线程环境执行，使用默认 RedisConnectionFactory Bean[name: '{}']", defaultRedisConnectionFactoryBeanName);
            return defaultRedisConnectionFactory;
        }
        logger.debug("开始切换目标 RedisConnectionFactory Bean[name : '{}']", targetBeanName);
        RedisConnectionFactory targetRedisConnectionFactory = getRedisConnectionFactory(targetBeanName);
        logger.debug("成功切换目标 RedisConnectionFactory Bean[name : '{}']", targetBeanName);
        return targetRedisConnectionFactory;
    }

    protected RedisConnectionFactory getRedisConnectionFactory(String beanName) {
        Map<String, RedisConnectionFactory> redisConnectionFactories = getRedisConnectionFactories();
        RedisConnectionFactory redisConnectionFactory = redisConnectionFactories.get(beanName);
        Assert.notNull(redisConnectionFactory, () -> FormatUtils.format("RedisConnectionFactory Bean[name : '{}'] 不存在", beanName));
        return redisConnectionFactory;
    }

    protected RedisConnectionFactory getDefaultRedisConnectionFactory() {
        if (defaultRedisConnectionFactory == null) {
            defaultRedisConnectionFactory = resolveDefaultRedisConnectionFactory();
        }
        return defaultRedisConnectionFactory;
    }

    protected Map<String, RedisConnectionFactory> getRedisConnectionFactories() {
        if (redisConnectionFactories == null) {
            redisConnectionFactories = resolveRedisConnectionFactories();
        }
        return redisConnectionFactories;
    }

    private RedisConnectionFactory resolveDefaultRedisConnectionFactory() {
        String beanName = defaultRedisConnectionFactoryBeanName;
        Assert.isTrue(hasText(beanName), "默认 RedisConnectionFactory Bean Name 不能未空");
        return getRedisConnectionFactory(beanName);
    }

    private Map<String, RedisConnectionFactory> resolveRedisConnectionFactories() {
        Map<String, RedisConnectionFactory> redisConnectionFactories = new HashMap<>(context.getBeansOfType(RedisConnectionFactory.class));
        // 移除当前 Bean
        redisConnectionFactories.remove(beanName);
        Assert.notEmpty(redisConnectionFactories, "RedisConnectionFactory Beans 不存在");
        return Collections.unmodifiableMap(redisConnectionFactories);
    }

    /**
     * 切换目标 {@link RedisConnectionFactory}
     * 请仔细阅读以下提示：
     * <ul>
     *     <li>当该方法调用后，请主动调用 {@link #clearTarget()} 方法，清除标记状态，特别在非 Web 请求线程场景中，以免内存泄漏分享（尽管框架会在 HTTP 请求结束时清除）</li>
     *     <li>当该方法调用如果在自定义线程池场景时，请注意复制当前 ThreadLocal 缓存的 <code>redisConnectionFactoryBeanName</code> 到目标线程中</li>
     *     <li>当 <code>redisConnectionFactoryBeanName</code> 所指向的 Bean 在当前应用上下文不存在时，会抛出异常</li>
     * </ul>
     *
     * @param redisConnectionFactoryBeanName 目标 {@link RedisConnectionFactory} Bean 名称
     */
    public static void switchTarget(String redisConnectionFactoryBeanName) {
        beanNameHolder.set(redisConnectionFactoryBeanName);
        logger.debug("切换目标 RedisConnectionFactory Bean Name : '{}'", redisConnectionFactoryBeanName);
    }

    public static void clearTarget() {
        String targetBeanName = getTargetBeanName();
        beanNameHolder.remove();
        logger.debug("清除目标 RedisConnectionFactory Bean Name : '{}'", targetBeanName);
    }

    protected static String getTargetBeanName() {
        return beanNameHolder.get();
    }
}
