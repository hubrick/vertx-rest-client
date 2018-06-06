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
package com.hubrick.vertx.rest.exception;

import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.impl.DefaultRestClientResponse;
import io.netty.buffer.ByteBuf;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.StreamBase;

import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public abstract class HttpStatusCodeException extends RestClientException {

    private static final long serialVersionUID = -5807494703720513267L;

    private static final String DEFAULT_CHARSET = "ISO-8859-1";

    private final StreamBase streamBase;
    private final HttpInputMessage httpInputMessage;
    private final List<HttpMessageConverter> httpMessageConverters;

    protected HttpStatusCodeException(StreamBase streamBase, HttpInputMessage httpInputMessage, List<HttpMessageConverter> httpMessageConverters) {
        super(httpInputMessage.getStatusMessage());
        checkNotNull(streamBase, "streamBase must not be null");
        checkNotNull(httpInputMessage, "httpInputMessage must not be null");
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");

        this.streamBase = streamBase;
        this.httpInputMessage = httpInputMessage;
        this.httpMessageConverters = httpMessageConverters;
    }


    /**
     * Return the HTTP status code.
     */
    public Integer getStatusCode() {
        return httpInputMessage.getStatusCode();
    }

    /**
     * Return the HTTP status text.
     */
    public String getStatusMessage() {
        return httpInputMessage.getStatusMessage();
    }

    /**
     * Return the HTTP response headers.
     *
     * @since 3.1.2
     */
    public MultiMap getResponseHeaders() {
        return httpInputMessage.getHeaders();
    }

    /**
     * Return the response body as a byte array.
     *
     * @since 3.0.5
     */
    public byte[] getResponseBodyAsByteArray() {
        final ByteBuf bodyByteBuf = httpInputMessage.getBody();
        final byte[] bytes = new byte[bodyByteBuf.readableBytes()];
        bodyByteBuf.readBytes(bytes);
        return bytes;
    }

    /**
     * Return the response body converted to the given class
     *
     * @since 3.0.5
     */
    public <T> T getResponseBody(Class<T> clazz) {
        final ByteBuf byteBuf = httpInputMessage.getBody();
        if(byteBuf.readableBytes() == 0 || Void.class.isAssignableFrom(clazz)) return null;

        final MediaType mediaType = MediaType.parseMediaType(httpInputMessage.getHeaders().get(HttpHeaders.CONTENT_TYPE));
        for (HttpMessageConverter httpMessageConverter : httpMessageConverters) {
            if (httpMessageConverter.canRead(clazz, mediaType)) {
                return (T) httpMessageConverter.read(clazz, httpInputMessage);
            }
        }
        return null;
    }

    /**
     * Return the response body as a string.
     *
     * @since 3.0.5
     */
    public String getResponseBodyAsString() {
        final ByteBuf bodyByteBuf = httpInputMessage.getBody();
        final String contentEncoding = httpInputMessage.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        byte[] bytes = new byte[bodyByteBuf.readableBytes()];
        bodyByteBuf.readBytes(bytes);
        return new String(bytes, contentEncoding != null ? Charset.forName(contentEncoding) : Charset.forName(DEFAULT_CHARSET));
    }
}
