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
package com.hubrick.vertx.rest.rx.impl;

import com.hubrick.vertx.rest.RestClient;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.common.DefaultRxRestClientResponse;
import com.hubrick.vertx.rest.rx.RxRestClient;
import com.hubrick.vertx.rest.common.DefaultRxRestClientRequest;
import io.vertx.core.http.HttpMethod;
import rx.Single;
import rx.functions.Action1;

/**
 * @author Emir Dizdarevic
 * @since 1.1.0
 */
public class DefaultRxRestClient implements RxRestClient {

    private final RestClient restClient;

    public DefaultRxRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Single<RestClientResponse<Void>> get(String uri, Action1<RestClientRequest<Void>> requestBuilder) {
        return request(HttpMethod.GET, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Single<RestClientResponse<T>> get(String uri, Class<T> responseClass, Action1<RestClientRequest<T>> requestBuilder) {
        return request(HttpMethod.GET, uri, responseClass, requestBuilder);
    }

    @Override
    public Single<RestClientResponse<Void>> post(String uri, Action1<RestClientRequest<Void>> requestBuilder) {
        return request(HttpMethod.POST, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Single<RestClientResponse<T>> post(String uri, Class<T> responseClass, Action1<RestClientRequest<T>> requestBuilder) {
        return request(HttpMethod.POST, uri, responseClass, requestBuilder);
    }

    @Override
    public Single<RestClientResponse<Void>> put(String uri, Action1<RestClientRequest<Void>> requestBuilder) {
        return request(HttpMethod.PUT, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Single<RestClientResponse<T>> put(String uri, Class<T> responseClass, Action1<RestClientRequest<T>> requestBuilder) {
        return request(HttpMethod.PUT, uri, responseClass, requestBuilder);
    }

    @Override
    public Single<RestClientResponse<Void>> delete(String uri, Action1<RestClientRequest<Void>> requestBuilder) {
        return request(HttpMethod.DELETE, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Single<RestClientResponse<T>> delete(String uri, Class<T> responseClass, Action1<RestClientRequest<T>> requestBuilder) {
        return request(HttpMethod.DELETE, uri, responseClass, requestBuilder);
    }

    @Override
    public Single<RestClientResponse<Void>> request(HttpMethod method, String uri, Action1<RestClientRequest<Void>> requestBuilder) {
        return request(method, uri, Void.class, requestBuilder);
    }

    /**
     * Creates Single based on emitter object. Emitter activated on subscribe() call and then configured to:
     * - produce event+complete on successful execution of REST call
     * - produce error if something goes wrong during construction OR call execution
     *
     * Also RestClientRequest is wrapped in custom class that denies override of exception handler
     * to make sure error is handled in rx way
     *
     * @return "cold" Single that emits rest call result
     */
    @Override
    public <T> Single<RestClientResponse<T>> request(HttpMethod method, String uri, Class<T> responseClass, Action1<RestClientRequest<T>> requestBuilder) {
        return Single.fromEmitter(emitter -> {
            final RestClientRequest<T> callbackRequest = restClient.request(method, uri, responseClass, restClientResponse -> emitter.onSuccess(new DefaultRxRestClientResponse<>(restClientResponse)))
                    .exceptionHandler(emitter::onError);
            try {
                final DefaultRxRestClientRequest<T> rxDecoratedRequest = new DefaultRxRestClientRequest<>(callbackRequest);
                requestBuilder.call(rxDecoratedRequest);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
