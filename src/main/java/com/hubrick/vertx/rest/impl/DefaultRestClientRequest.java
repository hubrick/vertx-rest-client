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
import com.hubrick.vertx.rest.*;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.exception.HttpClientErrorException;
import com.hubrick.vertx.rest.exception.HttpServerErrorException;
import com.hubrick.vertx.rest.exception.RestClientException;
import com.hubrick.vertx.rest.message.BufferedHttpOutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.impl.Base64;

import javax.annotation.Nullable;
import java.util.*;
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
    private final BufferedHttpOutputMessage bufferedHttpOutputMessage = new BufferedHttpOutputMessage();
    private final List<HttpMessageConverter> httpMessageConverters;
    private final HttpClientRequest httpClientRequest;
    private final MultiMap globalHeaders;
    private Handler<Throwable> exceptionHandler;

    private boolean headersCopied = false;
    private boolean globalHeadersPopulated = false;

    public DefaultRestClientRequest(HttpClient httpClient,
                                    List<HttpMessageConverter> httpMessageConverters,
                                    HttpMethod method,
                                    String uri,
                                    Class<T> responseClass,
                                    Handler<RestClientResponse<T>> responseHandler,
                                    int timeoutInMillis,
                                    MultiMap globalHeaders,
                                    @Nullable Handler<Throwable> exceptionHandler) {
        checkNotNull(httpClient, "httpClient must not be null");
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");
        checkNotNull(globalHeaders, "globalHeaders must not be null");

        this.httpClient = httpClient;
        this.httpMessageConverters = httpMessageConverters;
        this.exceptionHandler = exceptionHandler;
        this.globalHeaders = globalHeaders;

        httpClientRequest = httpClient.request(method.toString(), uri, (httpClientResponse) -> {
            handleResponse(httpClientResponse, responseClass, responseHandler);
        });

        if (timeoutInMillis > 0) {
            httpClientRequest.setTimeout(timeoutInMillis);
        }

        if (exceptionHandler != null) {
            httpClientRequest.exceptionHandler(exceptionHandler);
        }
    }

    private void handleResponse(HttpClientResponse httpClientResponse, Class clazz, Handler<RestClientResponse<T>> handler) {
        final Integer firstStatusDigit = httpClientResponse.statusCode() / 100;
        if (firstStatusDigit == 4 || firstStatusDigit == 5) {
            httpClientResponse.bodyHandler((buffer) -> {
                if (log.isWarnEnabled()) {
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
                } else {
                    throw exception;
                }
            });
        } else {
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
                    } else {
                        throw t;
                    }
                }
            });
        }
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
        return bufferedHttpOutputMessage.getHeaders();
    }

    @Override
    public RestClientRequest putHeader(String name, String value) {
        bufferedHttpOutputMessage.getHeaders().add(name, value);
        return this;
    }

    @Override
    public RestClientRequest putHeader(CharSequence name, CharSequence value) {
        bufferedHttpOutputMessage.getHeaders().add(name, value);
        return this;
    }

    @Override
    public RestClientRequest putHeader(String name, Iterable<String> values) {
        bufferedHttpOutputMessage.getHeaders().add(name, values);
        return this;
    }

    @Override
    public RestClientRequest putHeader(CharSequence name, Iterable<CharSequence> values) {
        bufferedHttpOutputMessage.getHeaders().add(name, values);
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
        populateGlobalHeaders();
        populateAcceptHeaderIfNotPresent();
        copyHeadersToHttpClientRequest();
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
        handleRequest(null, true);
    }

    @Override
    public RestClientRequest setTimeout(long timeoutMs) {
        httpClientRequest.setTimeout(timeoutMs);
        return this;
    }

    @Override
    public void setContentType(MediaType contentType) {
        bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType.toString());
    }

    @Override
    public MediaType getContentType() {
        return MediaType.parseMediaType(bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void setAcceptHeader(List<MediaType> mediaTypes) {
        bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.ACCEPT, formatForAcceptHeader(mediaTypes));
    }

    @Override
    public List<MediaType> getAcceptHeader() {
        final String acceptHeader = bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.ACCEPT);
        if (Strings.isNullOrEmpty(acceptHeader)) {
            return Collections.emptyList();
        } else {
            return FluentIterable.from(Splitter.on(",").split(acceptHeader)).toList().stream().map(MediaType::parseMediaType).collect(Collectors.toList());
        }
    }

    @Override
    public void setBasicAuth(String userPassCombination) {
        bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBytes(userPassCombination.getBytes(Charsets.UTF_8)));
    }

    @Override
    public String getBasicAuth() {
        final String base64UserPassCombination = bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.AUTHORIZATION);
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
                if (endRequest) {
                    if (!httpClientRequest.isChunked() && Strings.isNullOrEmpty(bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.CONTENT_LENGTH))) {
                        bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_LENGTH, "0");
                    }

                    copyHeadersToHttpClientRequest();
                    httpClientRequest.end();
                }
            } else {
                final Class<?> requestType = requestObject.getClass();
                final MediaType requestContentType = getContentType();

                for (HttpMessageConverter httpMessageConverter : httpMessageConverters) {
                    if (httpMessageConverter.canWrite(requestType, requestContentType)) {
                        httpMessageConverter.write(requestObject, requestContentType, bufferedHttpOutputMessage);

                        if (endRequest) {
                            copyHeadersToHttpClientRequest();
                            httpClientRequest.end(new Buffer(bufferedHttpOutputMessage.getBody()));
                            logRequest();
                        }
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
                log.error("No exceptionHandler found to handler exception.", t);
            }
        }
    }

    private void logRequest() {
        if (log.isDebugEnabled()) {
            final StringBuilder stringBuilder = new StringBuilder(256);
            for (String headerName : httpClientRequest.headers().names()) {
                for (String headerValue : httpClientRequest.headers().getAll(headerName)) {
                    stringBuilder.append(headerName);
                    stringBuilder.append(":");
                    stringBuilder.append(" ");
                    stringBuilder.append(headerValue);
                    stringBuilder.append("\r\n");
                }
            }
            stringBuilder.append("\r\n");
            stringBuilder.append(new String(bufferedHttpOutputMessage.getBody(), Charsets.UTF_8));

            log.debug("HTTP Request: \n{}", stringBuilder.toString());
        }
    }

    private void copyHeadersToHttpClientRequest() {
        if(!headersCopied) {
            for (Map.Entry<String, String> header : bufferedHttpOutputMessage.getHeaders()) {
                httpClientRequest.putHeader(header.getKey(), header.getValue());
            }
            headersCopied = true;
        }
    }

    private void populateGlobalHeaders() {
        if(!globalHeadersPopulated) {
            for (Map.Entry<String, String> header : globalHeaders) {
                httpClientRequest.putHeader(header.getKey(), header.getValue());
            }
            globalHeadersPopulated = true;
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
