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
package io.github.microsphere.spring.redis.serializer;

import io.github.microsphere.spring.redis.event.RedisCommandEvent;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.FastByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static io.github.microsphere.spring.redis.event.RedisCommandEvent.SERIALIZATION_VERSION;
import static io.github.microsphere.spring.redis.event.RedisCommandEvent.VERSION_1;
import static io.github.microsphere.spring.redis.serializer.RedisCommandEventSerializer.VersionedRedisSerializer.valueOf;
import static io.github.microsphere.spring.redis.serializer.Serializers.defaultSerializer;

/**
 * {@link RedisSerializer} for {@link RedisCommandEvent}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class RedisCommandEventSerializer implements RedisSerializer<RedisCommandEvent> {

    private static final RedisSerializer<RedisCommandEvent> delegate = findDelegate();

    private static RedisSerializer<RedisCommandEvent> findDelegate() {
        return valueOf(SERIALIZATION_VERSION);
    }

    @Override
    public byte[] serialize(RedisCommandEvent redisCommandEvent) throws SerializationException {
        if (redisCommandEvent == null) {
            return null;
        }
        return delegate.serialize(redisCommandEvent);
    }

    @Override
    public RedisCommandEvent deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length < 1) {
            return null;
        }
        return delegate.deserialize(bytes);
    }


    enum VersionedRedisSerializer implements RedisSerializer<RedisCommandEvent> {

        DEFAULT(-1) {
            @Override
            public byte[] serialize(RedisCommandEvent redisCommandEvent) throws SerializationException {
                return defaultSerializer.serialize(redisCommandEvent);
            }

            @Override
            public RedisCommandEvent deserialize(byte[] bytes) throws SerializationException {
                return (RedisCommandEvent) defaultSerializer.deserialize(bytes);
            }
        },

        V1(VERSION_1) {

            private final Charset asciiCharset = StandardCharsets.US_ASCII;

            @Override
            public byte[] serialize(RedisCommandEvent redisCommandEvent) throws SerializationException {
                FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream();
                try {
                    // write version
                    writeVersion(redisCommandEvent, outputStream);
                    // write interfaceName
                    writeInterfaceName(redisCommandEvent, outputStream);
                    // write methodName
                    writeMethodName(redisCommandEvent, outputStream);
                    // write parameter types;
                    writeParameterTypes(redisCommandEvent, outputStream);
                    // write parameters
                    writeParameters(redisCommandEvent, outputStream);
                    // write source application
                    writeSourceApplication(redisCommandEvent, outputStream);
                } catch (IOException e) {
                    throw new SerializationException("RedisCommandEvent serialization failed", e);
                } finally {
                    outputStream.close();
                }
                return outputStream.toByteArray();
            }

            private void writeVersion(RedisCommandEvent redisCommandEvent, FastByteArrayOutputStream outputStream) throws IOException {
                byte version = SERIALIZATION_VERSION;
                outputStream.write(version);
            }

            private void writeInterfaceName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
                writeString(redisCommandEvent::getInterfaceName, outputStream);
            }

            private void writeMethodName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
                writeString(redisCommandEvent::getMethodName, outputStream);
            }

            private void writeParameterTypes(RedisCommandEvent redisCommandEvent, FastByteArrayOutputStream outputStream) throws IOException {
                String[] parameterTypes = redisCommandEvent.getParameterTypes();
                int parameterCount = redisCommandEvent.getParameterCount();
                // write parameter count
                outputStream.write(parameterCount);
                // write each parameter type
                for (String parameterType : parameterTypes) {
                    writeString(parameterType, outputStream);
                }
            }

            private void writeParameters(RedisCommandEvent redisCommandEvent, FastByteArrayOutputStream outputStream) throws IOException {
                byte[][] parameters = redisCommandEvent.getParameters();
                int parameterCount = parameters.length;
                for (int i = 0; i < parameterCount; i++) {
                    byte[] parameter = parameters[i];
                    writeBytes(parameter, outputStream);
                }
            }

            private void writeSourceApplication(RedisCommandEvent redisCommandEvent, FastByteArrayOutputStream outputStream) throws IOException {
                writeString(redisCommandEvent::getSourceApplication, outputStream);
            }

            private void writeString(Supplier<String> supplier, OutputStream outputStream) throws IOException {
                String value = supplier.get();
                writeString(value, outputStream);
            }

            private void writeString(String value, OutputStream outputStream) throws IOException {
                byte[] bytes = getAsciiBytes(value);
                writeBytes(bytes, outputStream);
            }

            private void writeBytes(byte[] bytes, OutputStream outputStream) throws IOException {
                int bytesLength = bytes.length;
                outputStream.write(bytesLength);
                outputStream.write(bytes);
            }

            private byte[] getAsciiBytes(String value) {
                return value.getBytes(asciiCharset);
            }

            @Override
            public RedisCommandEvent deserialize(byte[] bytes) throws SerializationException {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                RedisCommandEvent redisCommandEvent = null;
                try {
                    // read version
                    int version = inputStream.read();
                    // read interfaceName
                    String interfaceName = readInterfaceName(inputStream);
                    // read methodName
                    String methodName = readMethodName(inputStream);
                    // read parameter types
                    int parameterCount = inputStream.read();
                    String[] parameterTypes = readParameterTypes(inputStream, parameterCount);
                    // read parameters
                    byte[][] parameters = readParameters(inputStream, parameterCount);
                    // read source application
                    String sourceApplication = readSourceApplication(inputStream);
                    redisCommandEvent = new RedisCommandEvent(interfaceName, methodName, parameterTypes, parameters, sourceApplication);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return redisCommandEvent;
            }

            private String readInterfaceName(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }

            private String readMethodName(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }

            private String[] readParameterTypes(InputStream inputStream, int parameterCount) throws IOException {
                String[] parameterTypes = new String[parameterCount];
                for (int i = 0; i < parameterCount; i++) {
                    parameterTypes[i] = readString(inputStream);
                }
                return parameterTypes;
            }

            private byte[][] readParameters(InputStream inputStream, int parameterCount) throws IOException {
                byte[][] parameters = new byte[parameterCount][];
                for (int i = 0; i < parameterCount; i++) {
                    parameters[i] = readBytes(inputStream);
                }
                return parameters;
            }

            private byte[] readBytes(InputStream inputStream) throws IOException {
                int length = inputStream.read();
                byte[] bytes = new byte[length];
                inputStream.read(bytes, 0, length);
                return bytes;
            }

            private String readSourceApplication(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }

            private String readString(InputStream inputStream) throws IOException {
                int length = inputStream.read();
                byte[] bytes = new byte[length];
                inputStream.read(bytes, 0, length);
                return new String(bytes, asciiCharset);
            }
        };

        private final byte version;

        VersionedRedisSerializer(int version) {
            this((byte) version);
        }

        VersionedRedisSerializer(byte version) {
            this.version = version;
        }

        static RedisSerializer<RedisCommandEvent> valueOf(byte version) {
            for (VersionedRedisSerializer redisSerializer : VersionedRedisSerializer.values()) {
                if (redisSerializer.version == version) {
                    return redisSerializer;
                }
            }
            return VersionedRedisSerializer.DEFAULT;
        }
    }
}
