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
package io.github.microsphere.spring.redis.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * The constants of Redis
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface RedisConstants {

    /**
     * {@link RedisTemplate} Source identification
     */
    byte REDIS_TEMPLATE_SOURCE = 1;

    /**
     * {@link StringRedisTemplate} source identification
     */
    byte STRING_REDIS_TEMPLATE_SOURCE = 2;

    /**
     * The custom {@link RedisTemplate} source identification
     * TODO: customization is not supported {@link RedisTemplate}
     */
    byte CUSTOMIZED_REDIS_TEMPLATE_SOURCE = 3;

}
