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

import static io.github.microsphere.spring.redis.event.RedisCommandEvent.SERIALIZATION_VERSION;
import static io.github.microsphere.spring.redis.serializer.RedisCommandEventSerializer.VersionedRedisSerializer.valueOf;
import static io.github.microsphere.spring.redis.serializer.Serializers.defaultSerializer;
import static io.github.microsphere.spring.redis.util.RedisCommandsUtils.resolveInterfaceName;
import static io.github.microsphere.spring.redis.util.RedisCommandsUtils.resolveSimpleInterfaceName;

/**
 * {@link RedisSerializer} for {@link RedisCommandEvent}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class RedisCommandEventSerializer extends AbstractSerializer<RedisCommandEvent> {

    public static final byte VERSION_DEFAULT = -1;

    public static final byte VERSION_1 = 1;

    public static final byte VERSION_2 = 2;

    private static final RedisSerializer<RedisCommandEvent> delegate = findDelegate();

    private static RedisSerializer<RedisCommandEvent> findDelegate() {
        return findDelegate(SERIALIZATION_VERSION);
    }

    private static RedisSerializer<RedisCommandEvent> findDelegate(byte version) {
        return valueOf(version);
    }

    @Override
    protected byte[] doSerialize(RedisCommandEvent redisCommandEvent) throws SerializationException {
        return delegate.serialize(redisCommandEvent);
    }

    @Override
    protected RedisCommandEvent doDeserialize(byte[] bytes) throws SerializationException {
        byte version = bytes[0];
        RedisSerializer<RedisCommandEvent> delegate = findDelegate(version);
        return delegate.deserialize(bytes);
    }

    enum VersionedRedisSerializer implements RedisSerializer<RedisCommandEvent> {

        DEFAULT(VERSION_DEFAULT) {
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
            @Override
            protected void writeMethodName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
                writeString(redisCommandEvent.getMethodName(), outputStream);
            }

            protected void writeType(String type, OutputStream outputStream) throws IOException {
                writeString(resolveSimpleInterfaceName(type), outputStream);
            }

            @Override
            protected String readType(InputStream inputStream) throws IOException {
                String type = readString(inputStream);
                return resolveInterfaceName(type);
            }

            @Override
            protected String readMethodName(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }
        },

        V2(VERSION_2) {
            @Override
            protected void writeMethodName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            }

            @Override
            protected String readInterfaceName(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }

            @Override
            protected String readMethodName(InputStream inputStream) throws IOException {
                return readString(inputStream);
            }

            @Override
            protected String[] readParameterTypes(InputStream inputStream, int parameterCount) throws IOException {
                String[] parameterTypes = new String[parameterCount];
                for (int i = 0; i < parameterCount; i++) {
                    parameterTypes[i] = readString(inputStream);
                }
                return parameterTypes;
            }
        };

        private final Charset asciiCharset = StandardCharsets.US_ASCII;

        private final byte version;

        VersionedRedisSerializer(int version) {
            this((byte) version);
        }

        VersionedRedisSerializer(byte version) {
            this.version = version;
        }

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

        @Override
        public RedisCommandEvent deserialize(byte[] bytes) throws SerializationException {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes, 1, bytes.length);
            RedisCommandEvent redisCommandEvent = null;
            try {
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

        protected void writeVersion(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            byte version = redisCommandEvent.getSerializationVersion();
            outputStream.write(version);
        }

        protected void writeInterfaceName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            writeType(redisCommandEvent.getInterfaceName(), outputStream);
        }

        protected void writeMethodName(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
        }

        protected void writeParameterTypes(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            String[] parameterTypes = redisCommandEvent.getParameterTypes();
            int parameterCount = redisCommandEvent.getParameterCount();
            // write parameter count
            outputStream.write(parameterCount);
            // write each parameter type
            for (String parameterType : parameterTypes) {
                writeType(parameterType, outputStream);
            }
        }

        protected void writeParameters(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            byte[][] parameters = redisCommandEvent.getParameters();
            int parameterCount = parameters.length;
            for (int i = 0; i < parameterCount; i++) {
                byte[] parameter = parameters[i];
                writeBytes(parameter, outputStream);
            }
        }

        protected void writeSourceApplication(RedisCommandEvent redisCommandEvent, OutputStream outputStream) throws IOException {
            writeString(redisCommandEvent.getSourceApplication(), outputStream);
        }

        protected void writeType(String type, OutputStream outputStream) throws IOException {
        }

        protected void writeString(String value, OutputStream outputStream) throws IOException {
            byte[] bytes = getBytes(value);
            writeBytes(bytes, outputStream);
        }

        protected void writeBytes(byte[] bytes, OutputStream outputStream) throws IOException {
            int bytesLength = bytes.length;
            outputStream.write(bytesLength);
            outputStream.write(bytes);
        }

        protected String readInterfaceName(InputStream inputStream) throws IOException {
            return readType(inputStream);
        }

        protected String readMethodName(InputStream inputStream) throws IOException {
            throw new UnsupportedOperationException();
        }

        protected String[] readParameterTypes(InputStream inputStream, int parameterCount) throws IOException {
            String[] parameterTypes = new String[parameterCount];
            for (int i = 0; i < parameterCount; i++) {
                parameterTypes[i] = readType(inputStream);
            }
            return parameterTypes;
        }

        protected byte[][] readParameters(InputStream inputStream, int parameterCount) throws IOException {
            byte[][] parameters = new byte[parameterCount][];
            for (int i = 0; i < parameterCount; i++) {
                parameters[i] = readBytes(inputStream);
            }
            return parameters;
        }

        protected String readSourceApplication(InputStream inputStream) throws IOException {
            return readString(inputStream);
        }

        protected byte[] readBytes(InputStream inputStream) throws IOException {
            int length = inputStream.read();
            byte[] bytes = new byte[length];
            inputStream.read(bytes, 0, length);
            return bytes;
        }

        protected String readString(InputStream inputStream) throws IOException {
            int length = inputStream.read();
            byte[] bytes = new byte[length];
            inputStream.read(bytes, 0, length);
            return new String(bytes, asciiCharset);
        }

        protected String readType(InputStream inputStream) throws IOException {
            throw new UnsupportedOperationException();
        }

        protected byte[] getBytes(String value) {
            return value.getBytes(asciiCharset);
        }

        static RedisSerializer<RedisCommandEvent> valueOf(byte version) {
            if (VERSION_1 == version) {
                return V1;
            } else if (VERSION_2 == version) {
                return V2;
            }
            return VersionedRedisSerializer.DEFAULT;
        }
    }
}
