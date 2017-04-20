/**
 * Copyright (C) 2015 Etaia AS (oss@hubrick.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.vertx.rest.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class JacksonJsonHttpMessageConverter<T extends Object> extends AbstractHttpMessageConverter<T> {

    private static final Logger log = LoggerFactory.getLogger(JacksonJsonHttpMessageConverter.class);
    private final ObjectMapper objectMapper;

    // Check for Jackson 2.3's overloaded canDeserialize/canSerialize variants with cause reference
    private static final boolean jackson23Available = getMethodIfAvailable(ObjectMapper.class, "canDeserialize", JavaType.class, AtomicReference.class) != null;

    public JacksonJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(new MediaType("application", "json", Charsets.UTF_8), new MediaType("application", "*+json", Charsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    private static Method getMethodIfAvailable(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        checkNotNull(clazz, "Class must not be null");
        checkNotNull(methodName, "Method name must not be null");
        if (paramTypes != null) {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        } else {
            Set<Method> candidates = new HashSet<>(1);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    candidates.add(method);
                }
            }
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            }
            return null;
        }
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        JavaType javaType = getJavaType(clazz, null);
        if (!jackson23Available) {
            return (this.objectMapper.canDeserialize(javaType) && canRead(mediaType));
        }
        AtomicReference<Throwable> causeRef = new AtomicReference<>();
        if (this.objectMapper.canDeserialize(javaType, causeRef) && canRead(mediaType)) {
            return true;
        }
        Throwable cause = causeRef.get();
        if (cause != null) {
            log.warn("Failed to evaluate deserialization for type {}", javaType, cause);
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!jackson23Available) {
            return (this.objectMapper.canSerialize(clazz) && canWrite(mediaType));
        }
        AtomicReference<Throwable> causeRef = new AtomicReference<Throwable>();
        if (this.objectMapper.canSerialize(clazz, causeRef) && canWrite(mediaType)) {
            return true;
        }
        Throwable cause = causeRef.get();
        if (cause != null) {
            log.warn("Failed to evaluate serialization for type [{}]", clazz, cause);
        }
        return false;
    }

    protected JavaType getJavaType(Type type, Class<?> contextClass) {
        return this.objectMapper.getTypeFactory().constructType(type, contextClass);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead/Write instead
        throw new UnsupportedOperationException();
    }

    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException {
        try {
            byte[] bytes = new byte[httpInputMessage.getBody().readableBytes()];
            httpInputMessage.getBody().readBytes(bytes);
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new HttpMessageConverterException("Error converting from json.", e);
        }
    }

    @Override
    protected void writeInternal(T object, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException {
        try {
            httpOutputMessage.write(Unpooled.unmodifiableBuffer(Unpooled.wrappedBuffer(objectMapper.writeValueAsBytes(object))));
        } catch (Exception e) {
            throw new HttpMessageConverterException("Error converting to json.", e);
        }

    }
}
