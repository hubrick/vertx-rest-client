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
import com.hubrick.vertx.rest.rx.RxRestClient;
import io.vertx.core.http.HttpMethod;
import rx.Observable;
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
    public Observable<RestClientResponse<Void>> get(String uri, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.GET, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Observable<RestClientResponse<T>> get(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.GET, uri, responseClass, requestBuilder);
    }

    @Override
    public Observable<RestClientResponse<Void>> post(String uri, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.POST, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Observable<RestClientResponse<T>> post(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.POST, uri, responseClass, requestBuilder);
    }

    @Override
    public Observable<RestClientResponse<Void>> put(String uri, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.PUT, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Observable<RestClientResponse<T>> put(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.PUT, uri, responseClass, requestBuilder);
    }

    @Override
    public Observable<RestClientResponse<Void>> delete(String uri, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.DELETE, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Observable<RestClientResponse<T>> delete(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder) {
        return request(HttpMethod.DELETE, uri, responseClass, requestBuilder);
    }

    @Override
    public Observable<RestClientResponse<Void>> request(HttpMethod method, String uri, Action1<RestClientRequest> requestBuilder) {
        return request(method, uri, Void.class, requestBuilder);
    }

    @Override
    public <T> Observable<RestClientResponse<T>> request(HttpMethod method, String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder) {
        final DefaultRxRestClientResponseMemoizeHandler<T> handler = new DefaultRxRestClientResponseMemoizeHandler<>();

        final RestClientRequest<T> originalRequest = restClient.request(method, uri, responseClass, handler);
        originalRequest.exceptionHandler(event -> handler.fail(event));
        final RestClientRequest<T> request = new DefaultRxRestClientRequest<>(originalRequest);

        // Use the builder to create the full request (or start upload)
        // We assume builder will call request.end()
        try {
            requestBuilder.call(request);
        } catch (Exception e) {
            // Request will never be sent so trigger error on the returned observable
            handler.fail(e);
        }

        return Observable.create(handler.getSubscribe());
    }
}
