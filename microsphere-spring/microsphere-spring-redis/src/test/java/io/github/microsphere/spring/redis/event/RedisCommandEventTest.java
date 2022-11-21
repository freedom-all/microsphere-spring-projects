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
package io.github.microsphere.spring.redis.event;

import io.github.microsphere.spring.redis.annotation.EnableRedisInterceptor;
import io.github.microsphere.spring.redis.interceptor.LoggingRedisCommandInterceptor;
import io.github.microsphere.spring.redis.interceptor.LoggingRedisConnectionInterceptor;
import io.github.microsphere.spring.test.redis.EnableRedisTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * {@link RedisCommandEvent} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        LoggingRedisConnectionInterceptor.class,
        LoggingRedisCommandInterceptor.class,
        RedisCommandEventTest.class
})
@TestPropertySource(properties = {
        "microsphere.redis.enabled=true"
})
@EnableRedisInterceptor
@EnableRedisTest
public class RedisCommandEventTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    public void test() {

        Map<Object, Object> data = new HashMap<>();
        context.addApplicationListener((ApplicationListener<RedisCommandEvent>) event -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
            Object key = keySerializer.deserialize((byte[]) event.getObjectParameter(0));
            Object value = valueSerializer.deserialize((byte[]) event.getObjectParameter(1));
            data.put(key, value);

            assertEquals("org.springframework.data.redis.connection.RedisStringCommands", event.getInterfaceName());
            assertEquals("set", event.getMethodName());
            assertArrayEquals(new String[]{"[B", "[B"}, event.getParameterTypes());
            assertEquals("default", event.getSourceApplication());
            assertEquals("redisConnectionFactory", event.getSourceBeanName());
        });

        redisTemplate.opsForValue().set("Key-1", "Value-1");
        assertEquals("Value-1", data.get("Key-1"));
    }
}
