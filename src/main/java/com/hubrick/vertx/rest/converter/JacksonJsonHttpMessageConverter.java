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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.MultiMap;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class JacksonJsonHttpMessageConverter<T extends Object> extends AbstractHttpMessageConverter<T> {

    private static final Logger log = LoggerFactory.getLogger(JacksonJsonHttpMessageConverter.class);
    private final ObjectMapper objectMapper;

    public JacksonJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(new MediaType("application", "json", Charsets.UTF_8), new MediaType("application", "*+json", Charsets.UTF_8));
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        JavaType javaType = getJavaType(clazz, null);
        AtomicReference<Throwable> causeRef = new AtomicReference<>();
        if (this.objectMapper.canDeserialize(javaType) && canRead(mediaType)) {
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
        AtomicReference<Throwable> causeRef = new AtomicReference<>();
        if (this.objectMapper.canSerialize(clazz) && canWrite(mediaType)) {
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
    protected T readInternal(Class<? extends T> clazz, byte[] buffer, MultiMap responseHeaders) throws HttpMessageConverterException {
        try {
            return objectMapper.readValue(buffer, clazz);
        } catch (IOException e) {
            throw new HttpMessageConverterException("Error converting from json.", e);
        }
    }

    @Override
    protected byte[] writeInternal(T object, MultiMap requestHeaders) throws HttpMessageConverterException {
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new HttpMessageConverterException("Error converting to json.", e);
        }
    }
}
