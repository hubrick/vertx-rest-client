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
package com.hubrick.vertx.rest.impl;

import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.exception.RestClientException;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.StreamBase;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation.
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class DefaultRestClientResponse<T> implements RestClientResponse<T> {

    private final List<HttpMessageConverter> httpMessageConverters;
    private final Class<T> clazz;
    private final HttpInputMessage httpInputMessage;
    private final StreamBase streamBase;
    private Handler<Throwable> exceptionHandler;

    DefaultRestClientResponse(List<HttpMessageConverter> httpMessageConverters,
                              Class<T> clazz,
                              HttpInputMessage httpInputMessage,
                              StreamBase streamBase,
                              @Nullable Handler<Throwable> exceptionHandler) {
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");
        checkNotNull(clazz, "clazz must not be null");
        checkNotNull(httpInputMessage, "httpInputMessage must not be null");
        checkNotNull(streamBase, "streamBase must not be null");

        this.httpMessageConverters = httpMessageConverters;
        this.clazz = clazz;
        this.httpInputMessage = httpInputMessage;
        this.streamBase = streamBase;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public int statusCode() {
        return httpInputMessage.getStatusCode();
    }

    @Override
    public String statusMessage() {
        return httpInputMessage.getStatusMessage();
    }

    @Override
    public MultiMap headers() {
        return httpInputMessage.getHeaders();
    }

    @Override
    public MultiMap trailers() {
        return httpInputMessage.getTrailers();
    }

    @Override
    public List<String> cookies() {
        return httpInputMessage.getCookies();
    }

    @Override
    public T getBody() {
        final ByteBuf byteBuf = httpInputMessage.getBody();
        if(byteBuf.readableBytes() == 0 || Void.class.isAssignableFrom(clazz)) return null;

        try {
            final MediaType mediaType = MediaType.parseMediaType(headers().get(HttpHeaders.CONTENT_TYPE));
            for (HttpMessageConverter httpMessageConverter : httpMessageConverters) {
                if (httpMessageConverter.canRead(clazz, mediaType)) {
                    return (T) httpMessageConverter.read(clazz, httpInputMessage);
                }
            }

            throw new RestClientException("Could not find any suitable DataMapper for reading media type " + mediaType);
        } catch (Throwable t) {
            if(exceptionHandler != null) {
                exceptionHandler.handle(t);
                return null;
            } else {
                throw t;
            }
        }
    }

    @Override
    public void exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        streamBase.exceptionHandler(exceptionHandler);
    }
}
