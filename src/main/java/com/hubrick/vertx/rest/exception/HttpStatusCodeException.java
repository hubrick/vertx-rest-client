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

import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.impl.DefaultRestClientResponse;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;

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

    private final HttpClientResponse httpClientResponse;
    private final List<HttpMessageConverter> httpMessageConverters;
    private final byte[] responseBody;

    protected HttpStatusCodeException(HttpClientResponse httpClientResponse, List<HttpMessageConverter> httpMessageConverters, byte[] responseBody) {
        super(httpClientResponse.statusMessage());
        checkNotNull(httpClientResponse, "httpClientResponse must not be null");
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");
        checkNotNull(responseBody, "responseBody must not be null");

        this.httpClientResponse = httpClientResponse;
        this.httpMessageConverters = httpMessageConverters;
        this.responseBody = responseBody;
    }


    /**
     * Return the HTTP status code.
     */
    public Integer getStatusCode() {
        return httpClientResponse.statusCode();
    }

    /**
     * Return the HTTP status text.
     */
    public String getStatusText() {
        return httpClientResponse.statusMessage();
    }

    /**
     * Return the HTTP response headers.
     *
     * @since 3.1.2
     */
    public MultiMap getResponseHeaders() {
        return httpClientResponse.headers();
    }

    /**
     * Return the response body as a byte array.
     *
     * @since 3.0.5
     */
    public byte[] getResponseBodyAsByteArray() {
        return this.responseBody;
    }

    /**
     * Return the response body as a byte array.
     *
     * @since 3.0.5
     */
    public <T> RestClientResponse<T> getResponseBody(Class<T> clazz) {
        return new DefaultRestClientResponse<>(httpMessageConverters, clazz, responseBody, httpClientResponse, null);
    }

    /**
     * Return the response body as a string.
     *
     * @since 3.0.5
     */
    public String getResponseBodyAsString() {
        final String contentEncoding = httpClientResponse.headers().get(HttpHeaders.CONTENT_ENCODING);
        return new String(this.responseBody, contentEncoding != null ? Charset.forName(contentEncoding) : Charset.forName(DEFAULT_CHARSET));
    }
}
