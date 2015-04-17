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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.hubrick.vertx.rest.HttpMethod;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.exception.HttpClientErrorException;
import com.hubrick.vertx.rest.exception.HttpServerErrorException;
import com.hubrick.vertx.rest.exception.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.impl.Base64;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation.
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class DefaultRestClientRequest<T> implements RestClientRequest<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultRestClientRequest.class);

    private final HttpClient httpClient;
    private final List<HttpMessageConverter> httpMessageConverters;
    private final HttpClientRequest httpClientRequest;
    private Handler<Throwable> exceptionHandler;

    public DefaultRestClientRequest(HttpClient httpClient,
                                    List<HttpMessageConverter> httpMessageConverters,
                                    HttpMethod method,
                                    String uri,
                                    Class<T> responseClass,
                                    Handler<RestClientResponse<T>> responseHandler,
                                    int timeoutInMillis,
                                    @Nullable Handler<Throwable> exceptionHandler) {
        checkNotNull(httpClient, "httpClient must not be null");
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");

        this.httpClient = httpClient;
        this.httpMessageConverters = httpMessageConverters;
        this.exceptionHandler = exceptionHandler;

        httpClientRequest = httpClient.request(method.toString(), uri, (httpClientResponse) -> {
            handleResponse(httpClientResponse, responseClass, responseHandler);
        });

        if (timeoutInMillis > 0) {
            httpClientRequest.setTimeout(timeoutInMillis);
        }

        if(exceptionHandler != null) {
            httpClientRequest.exceptionHandler(exceptionHandler);
        }
    }

    private void handleResponse(HttpClientResponse httpClientResponse, Class clazz, Handler<RestClientResponse<T>> handler) {
        final Integer firstStatusDigit = httpClientResponse.statusCode() / 100;
        if (firstStatusDigit == 4 || firstStatusDigit == 5) {
            httpClientResponse.bodyHandler((buffer) -> {
                if (log.isDebugEnabled()) {
                    final String body = new String(buffer.getBytes(), Charsets.UTF_8);
                    log.warn("Http request FAILED. Return status: {}, message: {}, body: {}", new Object[]{httpClientResponse.statusCode(), httpClientResponse.statusMessage(), body});
                }

                RuntimeException exception = null;
                switch (firstStatusDigit) {
                    case 4:
                        exception = new HttpClientErrorException(httpClientResponse, httpMessageConverters, buffer.getBytes());
                        break;
                    case 5:
                        exception = new HttpServerErrorException(httpClientResponse, httpMessageConverters, buffer.getBytes());
                        break;
                }
                if (exceptionHandler != null) {
                    exceptionHandler.handle(exception);
                    return;
                } else {
                    throw exception;
                }
            });
        }

        httpClientResponse.bodyHandler((buffer) -> {
            if (log.isDebugEnabled()) {
                final String body = new String(buffer.getBytes(), Charsets.UTF_8);
                log.debug("Http request SUCCESSFUL. Return status: {}, message: {}, body: {}", new Object[]{httpClientResponse.statusCode(), httpClientResponse.statusMessage(), body});
            }

            try {
                final RestClientResponse<T> restClientResponse = new DefaultRestClientResponse(
                        httpMessageConverters,
                        clazz,
                        buffer.getBytes(),
                        httpClientResponse,
                        exceptionHandler
                );

                handler.handle(restClientResponse);
            } catch (Throwable t) {
                log.error("Failed invoking rest handler", t);
                if (exceptionHandler != null) {
                    exceptionHandler.handle(t);
                    return;
                } else {
                    throw t;
                }
            }
        });
    }

    @Override
    public RestClientRequest setChunked(boolean chunked) {
        httpClientRequest.setChunked(true);
        return this;
    }

    @Override
    public boolean isChunked() {
        return httpClientRequest.isChunked();
    }

    @Override
    public MultiMap headers() {
        return httpClientRequest.headers();
    }

    @Override
    public RestClientRequest putHeader(String name, String value) {
        httpClientRequest.putHeader(name, value);
        return this;
    }

    @Override
    public RestClientRequest putHeader(CharSequence name, CharSequence value) {
        httpClientRequest.putHeader(name, value);
        return this;
    }

    @Override
    public RestClientRequest putHeader(String name, Iterable<String> values) {
        httpClientRequest.putHeader(name, values);
        return this;
    }

    @Override
    public RestClientRequest putHeader(CharSequence name, Iterable<CharSequence> values) {
        httpClientRequest.putHeader(name, values);
        return this;
    }

    @Override
    public RestClientRequest write(Object requestObject) {
        handleRequest(requestObject, false);
        return this;
    }

    @Override
    public RestClientRequest continueHandler(Handler<Void> handler) {
        httpClientRequest.continueHandler(handler);
        return this;
    }

    @Override
    public RestClientRequest sendHead() {
        httpClientRequest.sendHead();
        return this;
    }


    @Override
    public void end(Object requestObject) {
        populateAcceptHeaderIfNotPresent();
        handleRequest(requestObject, true);
    }

    @Override
    public void end() {
        populateAcceptHeaderIfNotPresent();
        httpClientRequest.end();
    }

    @Override
    public RestClientRequest setTimeout(long timeoutMs) {
        httpClientRequest.setTimeout(timeoutMs);
        return this;
    }

    @Override
    public void setContentType(MediaType contentType) {
        httpClientRequest.headers().set(HttpHeaders.CONTENT_TYPE, contentType.toString());
    }

    @Override
    public MediaType getContentType() {
        return MediaType.parseMediaType(httpClientRequest.headers().get(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void setAcceptHeader(List<MediaType> mediaTypes) {
        httpClientRequest.headers().set(HttpHeaders.ACCEPT, formatForAcceptHeader(mediaTypes));
    }

    @Override
    public List<MediaType> getAcceptHeader() {
        final String acceptHeader = httpClientRequest.headers().get(HttpHeaders.ACCEPT);
        if (Strings.isNullOrEmpty(acceptHeader)) {
            return Collections.emptyList();
        } else {
            return FluentIterable.from(Splitter.on(",").split(acceptHeader)).toList().stream().map(MediaType::parseMediaType).collect(Collectors.toList());
        }
    }

    @Override
    public void setBasicAuth(String userPassCombination) {
        httpClientRequest.headers().set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBytes(userPassCombination.getBytes(Charsets.UTF_8)));
    }

    @Override
    public String getBasicAuth() {
        final String base64UserPassCombination = httpClientRequest.headers().get(HttpHeaders.AUTHORIZATION);
        if (base64UserPassCombination != null && base64UserPassCombination.startsWith("Basic")) {
            return new String(Base64.decode(base64UserPassCombination), Charsets.UTF_8).replaceFirst("Basic", "").trim();
        } else {
            return null;
        }
    }

    @Override
    public RestClientRequest exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        httpClientRequest.exceptionHandler(exceptionHandler);
        return this;
    }

    private void handleRequest(Object requestObject, Boolean endRequest) {
        try {
            if (requestObject == null) {
                final MultiMap httpHeaders = httpClientRequest.headers();
                if (Strings.isNullOrEmpty(httpHeaders.get(HttpHeaders.CONTENT_LENGTH))) {
                    httpClientRequest.putHeader(HttpHeaders.CONTENT_LENGTH, "0");
                }
            } else {
                final Class<?> requestType = requestObject.getClass();
                final MediaType requestContentType = getContentType();
                for (HttpMessageConverter httpMessageConverter : httpMessageConverters) {
                    if (httpMessageConverter.canWrite(requestType, requestContentType)) {
                        httpMessageConverter.write(requestObject, requestContentType, httpClientRequest, endRequest);
                        return;
                    }
                }

                String message = "Could not write request: no suitable HttpMessageConverter found for request type [" + requestType.getName() + "]";
                if (requestContentType != null) {
                    message += " and content type [" + requestContentType + "]";
                }

                throw new RestClientException(message);
            }
        } catch (Throwable t) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(t);
            } else {
                throw t;
            }
        }
    }

    private void populateAcceptHeaderIfNotPresent() {
        final String acceptHeader = httpClientRequest.headers().get(HttpHeaders.ACCEPT);
        if (Strings.isNullOrEmpty(acceptHeader)) {
            httpClientRequest.headers().set(
                    HttpHeaders.ACCEPT,
                    formatForAcceptHeader(
                            httpMessageConverters
                                    .stream()
                                    .map(HttpMessageConverter::getSupportedMediaTypes)
                                    .reduce(new LinkedList<>(), (a, b) -> {
                                                a.addAll(b);
                                                return a;
                                            }
                                    )
                    )
            );
        }
    }

    private String formatForAcceptHeader(List<MediaType> mediaTypes) {
        final List<MediaType> formattedMediaTypes = stripDownCharset(mediaTypes);
        MediaType.sortBySpecificity(formattedMediaTypes);
        return Joiner.on(",").join(formattedMediaTypes.stream().map(MediaType::toString).collect(Collectors.toList()));
    }

    private List<MediaType> stripDownCharset(List<MediaType> mediaTypes) {
        final List<MediaType> result = new ArrayList<MediaType>(mediaTypes.size());
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.getCharSet() != null) {
                mediaType = new MediaType(mediaType.getType(), mediaType.getSubtype());
            }
            result.add(mediaType);
        }
        return result;
    }
}
