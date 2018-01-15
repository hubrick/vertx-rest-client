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
import com.google.common.io.BaseEncoding;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RequestCacheOptions;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.exception.HttpClientErrorException;
import com.hubrick.vertx.rest.exception.HttpServerErrorException;
import com.hubrick.vertx.rest.exception.RestClientException;
import com.hubrick.vertx.rest.message.BufferedHttpInputMessage;
import com.hubrick.vertx.rest.message.BufferedHttpOutputMessage;
import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final Vertx vertx;
    private final DefaultRestClient restClient;
    private final HttpClient httpClient;
    private final HttpMethod method;
    private final String uri;
    private final BufferedHttpOutputMessage bufferedHttpOutputMessage = new BufferedHttpOutputMessage();
    private final List<HttpMessageConverter> httpMessageConverters;
    private final HttpClientRequest httpClientRequest;
    private final MultiMap globalHeaders;
    private final Handler<RestClientResponse<T>> responseHandler;
    private Handler<Throwable> exceptionHandler;

    private RequestCacheOptions requestCacheOptions;
    private Long timeoutInMillis;

    // Status variables
    private boolean headersCopied = false;
    private boolean globalHeadersPopulated = false;
    private MultiKey cacheKey;

    public DefaultRestClientRequest(Vertx vertx,
                                    DefaultRestClient restClient,
                                    HttpClient httpClient,
                                    List<HttpMessageConverter> httpMessageConverters,
                                    HttpMethod method,
                                    String uri,
                                    Class<T> responseClass,
                                    Handler<RestClientResponse<T>> responseHandler,
                                    Long timeoutInMillis,
                                    RequestCacheOptions requestCacheOptions,
                                    MultiMap globalHeaders,
                                    @Nullable Handler<Throwable> exceptionHandler) {
        checkNotNull(vertx, "vertx must not be null");
        checkNotNull(restClient, "restClient must not be null");
        checkNotNull(httpClient, "httpClient must not be null");
        checkNotNull(httpMessageConverters, "dataMappers must not be null");
        checkArgument(!httpMessageConverters.isEmpty(), "dataMappers must not be empty");
        checkNotNull(globalHeaders, "globalHeaders must not be null");

        this.vertx = vertx;
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.method = method;
        this.uri = uri;
        this.httpMessageConverters = httpMessageConverters;
        this.responseHandler = responseHandler;
        this.globalHeaders = globalHeaders;

        httpClientRequest = httpClient.request(method, uri, (HttpClientResponse httpClientResponse) -> {
            handleResponse(httpClientResponse, responseClass);
        });

        this.requestCacheOptions = requestCacheOptions;
        this.timeoutInMillis = timeoutInMillis;

        if (exceptionHandler != null) {
            exceptionHandler(exceptionHandler);
        }
    }

    @Override
    public RestClientRequest setChunked(boolean chunked) {
        // Ignore this for now since chunked mode is not supported
        //httpClientRequest.setChunked(true);
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
        checkArgument(timeoutMs >= 0, "timeoutMs must be greater or equal to 0");

        this.timeoutInMillis = timeoutMs;
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
    public RestClientRequest<T> setRequestCache(RequestCacheOptions requestCacheOptions) {
        this.requestCacheOptions = requestCacheOptions;
        return this;
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
        bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + BaseEncoding.base64().encode(userPassCombination.getBytes(Charsets.UTF_8)));
    }

    @Override
    public String getBasicAuth() {
        final String base64UserPassCombination = bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (base64UserPassCombination != null && base64UserPassCombination.startsWith("Basic")) {
            return new String(BaseEncoding.base64().decode(base64UserPassCombination), Charsets.UTF_8).replaceFirst("Basic", "").trim();
        } else {
            return null;
        }
    }

    @Override
    public RestClientRequest exceptionHandler(Handler<Throwable> exceptionHandler) {
        final Handler<Throwable> wrapped = (t) -> {
            log.warn("Error requesting {}: {}", uri, t.getMessage(), t);
            exceptionHandler.handle(t);
        };

        this.exceptionHandler = wrapped;
        httpClientRequest.exceptionHandler(wrapped);
        return this;
    }

    private void handleRequest(Object requestObject, Boolean endRequest) {
        try {
            if (requestObject == null) {
                if (endRequest) {
                    endRequest();
                }
            } else {
                final Class<?> requestType = requestObject.getClass();
                final MediaType requestContentType = getContentType();

                for (HttpMessageConverter httpMessageConverter : httpMessageConverters) {
                    if (httpMessageConverter.canWrite(requestType, requestContentType)) {
                        httpMessageConverter.write(requestObject, requestContentType, bufferedHttpOutputMessage);

                        if (endRequest) {
                            endRequest();
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

    private void endRequest() {
        copyHeadersToHttpClientRequest();
        writeContentLength();
        cacheKey = createCacheKey(uri, bufferedHttpOutputMessage.getHeaders(), bufferedHttpOutputMessage.getBody());

        evictBefore(cacheKey);
        evictAllBefore();

        log.debug("Calling uri: {} {}", method, uri);
        if (HttpMethod.GET.equals(method) && requestCacheOptions != null) {
            try {
                if (isEvicting(requestCacheOptions)) {
                    log.debug("Cache MISS. Proceeding with request for key {}", cacheKey);
                    finishRequest(Optional.of(cacheKey));
                } else {
                    final RestClientResponse cachedRestClientResponse = restClient.getRequestCache().get(cacheKey);
                    if (cachedRestClientResponse != null) {
                        log.debug("Cache HIT. Retrieving entry from cache for key {}", cacheKey);
                        resetExpires(cacheKey);
                        vertx.runOnContext(aVoid -> {
                            try {
                                responseHandler.handle(cachedRestClientResponse);
                            } catch (Throwable t) {
                                log.error("Failed invoking rest handler", t);
                                if (exceptionHandler != null) {
                                    exceptionHandler.handle(t);
                                } else {
                                    throw t;
                                }
                            }
                        });
                    } else if (restClient.getRunningRequests().containsKey(cacheKey) && !restClient.getRunningRequests().get(cacheKey).isEmpty()) {
                        log.debug("Cache FUTURE HIT for key {}", cacheKey);
                        restClient.getRunningRequests().put(cacheKey, this);
                    } else {
                        log.debug("Cache MISS. Proceeding with request for key {}", cacheKey);
                        exceptionHandler(createExceptionPropagatingHandler());
                        finishRequest(Optional.of(cacheKey));
                    }
                }
            } catch (Throwable t) {
                log.error("Failed invoking rest handler", t);
                if (exceptionHandler != null) {
                    exceptionHandler.handle(t);
                } else {
                    throw t;
                }
            }
        } else {
            finishRequest(Optional.empty());
        }
    }

    private Handler<Throwable> createExceptionPropagatingHandler() {
        final Handler<Throwable> originalExceptionHandler = this.exceptionHandler;
        return (t) -> {
            log.warn("Error on {}: {}, will propagate to all running requests for that uri", uri, t.getMessage(), t);
            final RestClientRequestSlice<T> restClientRequestSlice = getRestClientRequestSlice();
            for (DefaultRestClientRequest<T> entry : restClientRequestSlice.getRestClientRequestSlice()) {
                vertx.runOnContext(aVoid -> {
                    if (entry == this) {
                        originalExceptionHandler.handle(t);
                    } else if (entry.exceptionHandler != null && entry != this) {
                        log.warn("Propagating error on {}: {} to running request: {}", uri, t.getMessage(), t, entry);
                        entry.exceptionHandler.handle(t);
                    }
                });
            }
            restClient.getRunningRequests().get(cacheKey).removeAll(restClientRequestSlice.getRestClientRequestSlice());
        };
    }

    private void handleResponse(HttpClientResponse httpClientResponse, Class clazz) {
        final Integer firstStatusDigit = httpClientResponse.statusCode() / 100;
        if (firstStatusDigit == 4 || firstStatusDigit == 5) {
            httpClientResponse.bodyHandler((buffer) -> {
                httpClientResponse.exceptionHandler(null);
                if (log.isWarnEnabled()) {
                    final String body = new String(buffer.getBytes(), Charsets.UTF_8);
                    log.warn("Http request to {} FAILED. Return status: {}, message: {}, body: {}", new Object[]{uri, httpClientResponse.statusCode(), httpClientResponse.statusMessage(), body});
                }

                RuntimeException exception = null;
                switch (firstStatusDigit) {
                    case 4:
                        exception = new HttpClientErrorException(httpClientResponse, createHttpInputMessage(buffer.getByteBuf(), httpClientResponse), httpMessageConverters);
                        break;
                    case 5:
                        exception = new HttpServerErrorException(httpClientResponse, createHttpInputMessage(buffer.getByteBuf(), httpClientResponse), httpMessageConverters);
                        break;
                }
                handleException(exception);
            });
        } else {
            httpClientResponse.bodyHandler((buffer) -> {
                httpClientResponse.exceptionHandler(null);
                if (log.isDebugEnabled()) {
                    final String body = new String(buffer.getBytes(), Charsets.UTF_8);
                    log.debug("Http request to {} {} SUCCESSFUL. Return status: {}, message: {}, body: {}", new Object[]{method, uri, httpClientResponse.statusCode(), httpClientResponse.statusMessage(), body});
                }

                final RestClientResponse<T> restClientResponse = new DefaultRestClientResponse(
                        httpMessageConverters,
                        clazz,
                        createHttpInputMessage(buffer.getByteBuf(), httpClientResponse),
                        httpClientResponse,
                        exceptionHandler
                );

                handleResponse(restClientResponse);
            });
        }
    }

    private void handleResponse(RestClientResponse<T> restClientResponse) {
        if (HttpMethod.GET.equals(method) && requestCacheOptions != null) {
            final RestClientRequestSlice<T> restClientRequestSlice = getRestClientRequestSlice();

            if (restClientRequestSlice.isLastFiredRestClientRequest()) {
                cache(restClientResponse);
            }

            for (DefaultRestClientRequest<T> entry : restClientRequestSlice.getRestClientRequestSlice()) {
                vertx.runOnContext(aVoid -> {
                    try {
                        log.debug("Handling FUTURE HIT for key {} and restClientRequest {}", cacheKey, entry);
                        entry.responseHandler.handle(restClientResponse);
                    } catch (Throwable t) {
                        log.error("Failed invoking rest handler", t);
                        if (entry.exceptionHandler != null) {
                            entry.exceptionHandler.handle(t);
                        } else {
                            throw t;
                        }
                    }
                });
            }
            restClient.getRunningRequests().get(cacheKey).removeAll(restClientRequestSlice.getRestClientRequestSlice());
        } else {
            try {
                responseHandler.handle(restClientResponse);
            } catch (Throwable t) {
                log.error("Failed invoking rest handler", t);
                if (exceptionHandler != null) {
                    exceptionHandler.handle(t);
                } else {
                    throw t;
                }
            }
        }
    }

    private void handleException(RuntimeException exception) {
        if (exceptionHandler != null) {
            log.error("Http error. Handling exception", exception);
            exceptionHandler.handle(exception);
        } else {
            throw exception;
        }
    }

    private void cache(RestClientResponse restClientResponse) {
        if (HttpMethod.GET.equals(method) && requestCacheOptions != null && requestCacheOptions.getCachedStatusCodes().contains(restClientResponse.statusCode())) {
            log.debug("Caching entry with key {}", cacheKey);

            cancelOutstandingEvictionTimer(cacheKey);
            restClient.getRequestCache().put(cacheKey, restClientResponse);
            createEvictionTimer(cacheKey, requestCacheOptions.getExpiresAfterWriteMillis());
        }
    }

    private RestClientRequestSlice getRestClientRequestSlice() {
        boolean lastFiredRestClientRequest = false;
        final List<DefaultRestClientRequest<T>> restClientRequestsToHandle = new LinkedList<>();
        for (DefaultRestClientRequest<T> entry : restClient.getRunningRequests().get(cacheKey)) {
            if (entry == this) {
                lastFiredRestClientRequest = true;
            } else if (lastFiredRestClientRequest && isEvicting(entry.requestCacheOptions)) {
                lastFiredRestClientRequest = false;
                break;
            }

            if (lastFiredRestClientRequest) {
                restClientRequestsToHandle.add(entry);
            }
        }

        return new RestClientRequestSlice(restClientRequestsToHandle, lastFiredRestClientRequest);
    }

    private HttpInputMessage createHttpInputMessage(ByteBuf body, HttpClientResponse httpClientResponse) {
        return new BufferedHttpInputMessage(
                body,
                httpClientResponse.headers(),
                httpClientResponse.trailers(),
                httpClientResponse.statusMessage(),
                httpClientResponse.statusCode(),
                httpClientResponse.cookies()
        );
    }

    private void logRequest() {
        if (log.isDebugEnabled()) {
            final ByteBuf bodyByteBuf = bufferedHttpOutputMessage.getBody();
            byte[] bytes = new byte[bodyByteBuf.readableBytes()];
            bodyByteBuf.readBytes(bytes);

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
            stringBuilder.append(new String(bytes, Charsets.UTF_8));

            log.debug("HTTP Request: \n{}", stringBuilder.toString());
        }
    }

    private boolean isEvicting(RequestCacheOptions requestCacheOptions) {
        return requestCacheOptions.getEvictBefore() || requestCacheOptions.getEvictAllBefore();
    }

    private MultiKey createCacheKey(String uri, MultiMap headers, ByteBuf body) {
        return new MultiKey(
                uri,
                headers.entries().stream().map(e -> e.getKey() + ": " + e.getValue()).sorted().collect(Collectors.toList()),
                body.hashCode()
        );
    }

    private void evictBefore(MultiKey key) {
        if (requestCacheOptions != null && requestCacheOptions.getEvictBefore()) {
            log.debug("EVICTING entry from cache for key {}", key);
            cancelOutstandingEvictionTimer(key);
            restClient.getRequestCache().remove(key);
        }
    }

    private void resetExpires(MultiKey key) {
        if (requestCacheOptions.getExpiresAfterAccessMillis() > 0) {
            cancelOutstandingEvictionTimer(key);
            createEvictionTimer(key, requestCacheOptions.getExpiresAfterAccessMillis());
        }
    }

    private void evictAllBefore() {
        if (requestCacheOptions != null && requestCacheOptions.getEvictAllBefore()) {
            log.debug("EVICTING all entries from cache");
            restClient.getRequestCache().clear();
            restClient.getEvictionTimersCache().clear();
        }
    }

    private void cancelOutstandingEvictionTimer(MultiKey multiKey) {
        final Long outstandingTimer = restClient.getEvictionTimersCache().get(multiKey);
        if (outstandingTimer != null) {
            vertx.cancelTimer(outstandingTimer);
            restClient.getEvictionTimersCache().remove(multiKey);
        }
    }

    private void createEvictionTimer(MultiKey key, long ttl) {
        final long timerId = vertx.setTimer(ttl, timerIdRef -> {
            if (restClient.getEvictionTimersCache().containsValue(timerIdRef)) {
                log.debug("EVICTING entry from cache for key {}", key);
                restClient.getRequestCache().remove(key);
                restClient.getEvictionTimersCache().remove(key);
            }
        });
        restClient.getEvictionTimersCache().put(key, timerId);
    }

    private void finishRequest(Optional<MultiKey> key) {
        if (timeoutInMillis > 0) {
            httpClientRequest.setTimeout(timeoutInMillis);
        }
        httpClientRequest.end(Buffer.buffer(bufferedHttpOutputMessage.getBody()));
        key.ifPresent(e -> restClient.getRunningRequests().put(e, this));
        logRequest();
    }

    private void writeContentLength() {
        if (!httpClientRequest.isChunked() && Strings.isNullOrEmpty(bufferedHttpOutputMessage.getHeaders().get(HttpHeaders.CONTENT_LENGTH))) {
            bufferedHttpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(bufferedHttpOutputMessage.getBody().readableBytes()));
        }
    }

    private void copyHeadersToHttpClientRequest() {
        populateGlobalHeaders();
        if (!headersCopied) {
            for (Map.Entry<String, String> header : bufferedHttpOutputMessage.getHeaders()) {
                httpClientRequest.putHeader(header.getKey(), header.getValue());
            }
            headersCopied = true;
        }
    }

    private void populateGlobalHeaders() {
        if (!globalHeadersPopulated) {
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

    private static class RestClientRequestSlice<T> {

        private final List<DefaultRestClientRequest<T>> restClientRequestSlice;
        private final boolean lastFiredRestClientRequest;

        private RestClientRequestSlice(List<DefaultRestClientRequest<T>> restClientRequestSlice, boolean lastFiredRestClientRequest) {
            this.restClientRequestSlice = restClientRequestSlice;
            this.lastFiredRestClientRequest = lastFiredRestClientRequest;
        }

        public List<DefaultRestClientRequest<T>> getRestClientRequestSlice() {
            return restClientRequestSlice;
        }

        public boolean isLastFiredRestClientRequest() {
            return lastFiredRestClientRequest;
        }
    }
}
