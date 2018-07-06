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
package com.hubrick.vertx.rest.common;

import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RequestCacheOptions;
import com.hubrick.vertx.rest.RestClientRequest;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;

import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class DefaultRxRestClientRequest<T> implements RestClientRequest<T> {

    private final RestClientRequest<T> decorated;

    public DefaultRxRestClientRequest(RestClientRequest<T> decorated) {
        this.decorated = decorated;
    }

    @Override
    public RestClientRequest<T> exceptionHandler(Handler<Throwable> exceptionHandler) {
        throw new UnsupportedOperationException("This method is not supported. Use RxJava's doOnError method to handle exceptions");
    }

    @Override
    public RestClientRequest<T> setChunked(boolean chunked) {
        return decorated.setChunked(chunked);
    }

    @Override
    public boolean isChunked() {
        return decorated.isChunked();
    }

    @Override
    public MultiMap headers() {
        return decorated.headers();
    }

    @Override
    public RestClientRequest<T> putHeader(String name, String value) {
        return decorated.putHeader(name, value);
    }

    @Override
    public RestClientRequest<T> putHeader(CharSequence name, CharSequence value) {
        return decorated.putHeader(name, value);
    }

    @Override
    public RestClientRequest<T> putHeader(String name, Iterable<String> values) {
        return decorated.putHeader(name, values);
    }

    @Override
    public RestClientRequest<T> putHeader(CharSequence name, Iterable<CharSequence> values) {
        return decorated.putHeader(name, values);
    }

    @Override
    public RestClientRequest<T> write(Object requestObject) {
        return decorated.write(requestObject);
    }

    @Override
    public RestClientRequest<T> continueHandler(Handler<Void> handler) {
        return decorated.continueHandler(handler);
    }

    @Override
    public RestClientRequest<T> sendHead() {
        return decorated.sendHead();
    }

    @Override
    public void end(Object requestObject) {
        decorated.end(requestObject);
    }

    @Override
    public void end() {
        decorated.end();
    }

    @Override
    public RestClientRequest<T> setTimeout(long timeoutMs) {
        return decorated.setTimeout(timeoutMs);
    }

    @Override
    public void setContentType(MediaType contentType) {
        decorated.setContentType(contentType);
    }

    @Override
    public MediaType getContentType() {
        return decorated.getContentType();
    }

    @Override
    public RestClientRequest<T> setRequestCache(RequestCacheOptions requestCacheOptions) {
        return decorated.setRequestCache(requestCacheOptions);
    }

    @Override
    public void setAcceptHeader(List<MediaType> mediaTypes) {
        decorated.setAcceptHeader(mediaTypes);
    }

    @Override
    public List<MediaType> getAcceptHeader() {
        return decorated.getAcceptHeader();
    }

    @Override
    public void setBasicAuth(String userPassCombination) {
        decorated.setBasicAuth(userPassCombination);
    }

    @Override
    public String getBasicAuth() {
        return decorated.getBasicAuth();
    }
}
