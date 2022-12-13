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
package io.github.microsphere.spring.redis;

import io.github.microsphere.spring.redis.context.RedisContext;
import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Abstract {@link RedisCommandEvent} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@Disabled
public abstract class AbstractRedisCommandEventTest extends AbstractRedisTest {

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    protected ConfigurableApplicationContext context;

    @Autowired
    protected RedisContext redisContext;

    @Test
    public void test() {

        Map<Object, Object> data = new HashMap<>();
        context.addApplicationListener((ApplicationListener<RedisCommandEvent>) event -> {
            RedisSerializer keySerializer = stringRedisTemplate.getKeySerializer();
            RedisSerializer valueSerializer = stringRedisTemplate.getValueSerializer();
            byte[] parameter0 = event.getParameter(0);
            byte[] parameter1 = event.getParameter(1);
            Object key = keySerializer.deserialize(parameter0);
            Object value = valueSerializer.deserialize(parameter1);
            data.put(key, value);

            // assert interface name
            assertEquals("org.springframework.data.redis.connection.RedisStringCommands", event.getInterfaceName());

            // assert method name
            assertEquals("set", event.getMethodName());

            // assert parameters
            assertArrayEquals(new byte[][]{keySerializer.serialize(key), valueSerializer.serialize(value)}, event.getParameters());

            // assert object parameters
            assertArrayEquals(event.getObjectParameters(), event.getParameters());
            assertEquals(event.getObjectParameter(0), parameter0);
            assertEquals(event.getObjectParameter(1), parameter1);

            // assert parameter count
            assertEquals(2, event.getParameterCount());

            // assert parameter types
            assertArrayEquals(new String[]{"[B", "[B"}, event.getParameterTypes());
            assertArrayEquals(new Class[]{byte[].class, byte[].class}, event.getParameterClasses());
            assertArrayEquals(new Class[]{byte[].class, byte[].class}, event.getParameterClasses());
            assertSame(byte[].class, event.getParameterClass(0));
            assertSame(byte[].class, event.getParameterClass(1));

            // assert source application
            assertEquals("default", event.getSourceApplication());

            // assert ClassLoader
            assertSame(context.getClassLoader(), event.getClassLoader());

            // assert Redis Beans
            assertNotNull(event.getRedisMethodContext());
            assertSame(redisContext, event.getRedisContext());

            assertSame(context, event.getRedisContext().getApplicationContext());
        });

        stringRedisTemplate.opsForValue().set("Key-1", "Value-1");
        assertEquals("Value-1", data.get("Key-1"));
    }
}
