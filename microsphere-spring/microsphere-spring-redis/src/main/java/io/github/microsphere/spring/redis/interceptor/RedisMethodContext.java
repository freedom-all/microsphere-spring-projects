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
package io.github.microsphere.spring.redis.interceptor;

import io.github.microsphere.spring.redis.context.RedisContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * Redis Method Context
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class RedisMethodContext<T> {

    private final T target;

    private final Method method;

    private final Object[] args;

    private final RedisContext redisContext;

    private final String sourceBeanName;

    private long startTimeNanos = -1;

    private long durationNanos = -1;

    public RedisMethodContext(T target, Method method, Object[] args, RedisContext redisContext) {
        this(target, method, args, redisContext, null);
    }

    public RedisMethodContext(T target, Method method, Object[] args, RedisContext redisContext, String sourceBeanName) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.redisContext = redisContext;
        this.sourceBeanName = sourceBeanName;
    }

    public T getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getSourceBeanName() {
        return sourceBeanName;
    }

    public RedisContext getRedisContext() {
        return redisContext;
    }

    /**
     * Start and record the time in nano seconds, the initialized value is negative
     */
    public void start() {
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Stop and record the time in nano seconds, the initialized value is negative
     *
     * @throws IllegalStateException if {@link #start()} is not execute before
     */
    public void stop() {
        if (startTimeNanos < 0) {
            throw new IllegalStateException("'stop()' method must not be invoked before the execution of 'start()' method");
        }
        this.durationNanos = System.nanoTime() - startTimeNanos;
    }

    /**
     * Get the start time in nano seconds
     *
     * @return If the value is negative, it indicates {@link #start()} method was not executed
     */
    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    /**
     * Get the execution duration time of redis method in nano seconds
     *
     * @return If the value is negative, it indicates the duration can't not be evaluated,
     * because {@link #start()} method was not executed
     */
    public long getDurationNanos() {
        return durationNanos;
    }

    /**
     * Get the execution duration time of redis method in the specified {@link TimeUnit time unit}
     *
     * @return If the value is negative, it indicates the duration can't not be evaluated,
     * because {@link #start()} method was not executed
     */
    public long getDuration(TimeUnit timeUnit) {
        long durationNanos = getDurationNanos();
        if (durationNanos < 0) {
            return durationNanos;
        }
        return timeUnit.convert(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RedisMethodContext.class.getSimpleName() + "[", "]")
                .add("target=" + target)
                .add("method=" + method)
                .add("args=" + Arrays.toString(args))
                .add("redisContext=" + redisContext)
                .add("sourceBeanName='" + sourceBeanName + "'")
                .add("startTimeNanos=" + startTimeNanos)
                .add("duration=" + getDuration(TimeUnit.MILLISECONDS))
                .toString();
    }
}
