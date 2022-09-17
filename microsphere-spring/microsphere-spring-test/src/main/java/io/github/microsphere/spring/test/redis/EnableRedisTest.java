package io.github.microsphere.spring.test.redis;

import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;

import java.lang.annotation.*;

/**
 * 激活 Redis 测试
 * 激活组件列表：
 * <ul>
 *     <li>{@link RedisTemplate RedisTemplate&lt;Object,Object&gt;}</li>
 *     <li>{@link StringRedisTemplate}</li>
 *     <li>{@link RedisServer}</li>
 * </ul>
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RedisTestConfiguration.class)
public @interface EnableRedisTest {
}
