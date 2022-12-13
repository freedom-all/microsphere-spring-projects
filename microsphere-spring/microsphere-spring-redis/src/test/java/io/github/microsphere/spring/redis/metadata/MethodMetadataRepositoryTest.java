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
package io.github.microsphere.spring.redis.metadata;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link MethodMetadataRepository} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class MethodMetadataRepositoryTest {

    @Test
    public void testInit() {
        MethodMetadataRepository.init();
    }

    @Test
    public void test() {
        Set<String> methodNames = new TreeSet<>();
        Set<Type> types = new TreeSet<>(Comparator.comparing(Type::getTypeName));
        for (Method method : MethodMetadataRepository.getWriteCommandMethods()) {
            types.add(method.getDeclaringClass());
            for (Type parameterType : method.getGenericParameterTypes()) {
                types.addAll(findTypes(parameterType));
            }
            methodNames.add(method.getName());
        }

        types.forEach(type -> {
            System.out.println(type.getTypeName());
        });

        methodNames.forEach(System.out::println);

    }

    private Set<Type> findTypes(Type type) {
        Set<Type> types = new HashSet<>();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            types.add(pType.getRawType());
            for (Type t : pType.getActualTypeArguments()) {
                types.addAll(findTypes(t));
            }
        } else if (type instanceof Class) {
            types.add(type);
        }
        return types;
    }
}
