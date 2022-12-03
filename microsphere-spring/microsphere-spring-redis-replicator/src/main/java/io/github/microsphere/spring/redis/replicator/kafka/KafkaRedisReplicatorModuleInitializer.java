package io.github.microsphere.spring.redis.replicator.kafka;

import io.github.microsphere.spring.redis.replicator.RedisReplicatorModuleInitializer;
import io.github.microsphere.spring.redis.replicator.kafka.consumer.KafkaConsumerRedisReplicatorConfiguration;
import io.github.microsphere.spring.redis.replicator.kafka.producer.KafkaProducerRedisCommandEventListener;
import io.github.microsphere.spring.redis.replicator.kafka.producer.KafkaProducerRedisReplicatorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import static io.github.microsphere.spring.redis.replicator.kafka.consumer.KafkaConsumerRedisReplicatorConfiguration.KAFKA_CONSUMER_ENABLED_PROPERTY_NAME;

/**
 * Kafka {@link RedisReplicatorModuleInitializer}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public class KafkaRedisReplicatorModuleInitializer implements RedisReplicatorModuleInitializer {

    private static final String KAFKA_TEMPLATE_CLASS_NAME = "org.springframework.kafka.core.KafkaTemplate";

    private static final Logger logger = LoggerFactory.getLogger(KafkaRedisReplicatorModuleInitializer.class);

    @Override
    public boolean supports(ConfigurableApplicationContext context) {
        if (!isClassPresent(context)) {
            logger.warn("spring-kafka and its related artifacts are not found in the class-path of application context [id: '{}'] . " + "The Kafka module will not be enabled!", context.getId());
            return false;
        }
        if (!hasBootstrapServers(context)) {
            logger.warn("Application context [id: '{}'] If the spring-kafka server cluster address is not configured, " + "The Kafka module will not be enabled!", context.getId());
            return false;
        }
        return true;
    }

    @Override
    public void initializeProducerModule(ConfigurableApplicationContext context, BeanDefinitionRegistry registry) {
        registerConfiguration(context, registry, KafkaProducerRedisReplicatorConfiguration.class);
        initializeKafkaProducerRedisCommandEventListener(context);
    }

    @Override
    public void initializeConsumerModule(ConfigurableApplicationContext context, BeanDefinitionRegistry registry) {
        if (!KafkaConsumerRedisReplicatorConfiguration.isEnabled(context)) {
            logger.warn("Application context [id: '{}'] Redis Repliator Kafka Consumer is not activated, you can configure Spring property {} = true to enable!", context.getId(), KAFKA_CONSUMER_ENABLED_PROPERTY_NAME);
            return;
        }
        registerConfiguration(context, registry, KafkaConsumerRedisReplicatorConfiguration.class);
    }

    private boolean registerConfiguration(ConfigurableApplicationContext context, BeanDefinitionRegistry registry, Class<?> configClass) {
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry, context.getEnvironment());
        reader.register(configClass);
        logger.debug("Application context [id: '{}'] Registered Redis data replication Kafka configuration class: {}", context.getId(), configClass);
        return true;
    }

    private void initializeKafkaProducerRedisCommandEventListener(ConfigurableApplicationContext context) {
        KafkaProducerRedisCommandEventListener listener = new KafkaProducerRedisCommandEventListener();
        context.addApplicationListener(listener);
        logger.debug("Application context [id: '{}'] Listener added: {}", context.getId(), listener.getClass());
    }

    private boolean isClassPresent(ConfigurableApplicationContext context) {
        ClassLoader classLoader = context.getClassLoader();
        return ClassUtils.isPresent(KAFKA_TEMPLATE_CLASS_NAME, classLoader);
    }

    private boolean hasBootstrapServers(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        return environment.containsProperty(KafkaRedisReplicatorConfiguration.BOOTSTRAP_SERVERS_PROPERTY_NAME);
    }
}
