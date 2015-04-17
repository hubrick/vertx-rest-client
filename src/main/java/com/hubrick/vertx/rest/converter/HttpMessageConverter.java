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

import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;

import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface HttpMessageConverter<T> {

    T read(Class<? extends T> clazz, byte[] buffer, HttpClientResponse httpClientResponse) throws HttpMessageConverterException;
    void write(T object, MediaType contentType, HttpClientRequest httpClientRequest, boolean endRequest) throws HttpMessageConverterException;
    List<MediaType> getSupportedMediaTypes();
    boolean canRead(Class<?> clazz, MediaType mediaType);
    boolean canWrite(Class<?> clazz, MediaType mediaType);
}
