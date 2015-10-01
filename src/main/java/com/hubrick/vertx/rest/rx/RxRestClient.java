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
package com.hubrick.vertx.rest.rx;

import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import io.vertx.core.http.HttpMethod;
import rx.Observable;
import rx.functions.Action1;

/**
 * An RX wrapper around {@link com.hubrick.vertx.rest.RestClient}
 *
 * @author Emir Dizdarevic
 * @since 1.1.0
 */
public interface RxRestClient {

    /**
     * Makes a GET call with no response value.
     *
     * @param uri The uri which should be called.
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    Observable<RestClientResponse<Void>> get(String uri, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a GET call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    <T> Observable<RestClientResponse<T>> get(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a POST call with no response value.
     *
     * @param uri The uri which should be called.
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    Observable<RestClientResponse<Void>> post(String uri, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a POST call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    <T> Observable<RestClientResponse<T>> post(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a PUT call with no response value.
     *
     * @param uri The uri which should be called.
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    Observable<RestClientResponse<Void>> put(String uri, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a PUT call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    <T> Observable<RestClientResponse<T>> put(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a DELETE call with no response value.
     *
     * @param uri The uri which should be called.
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    Observable<RestClientResponse<Void>> delete(String uri, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a DELETE call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    <T> Observable<RestClientResponse<T>> delete(String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a GET, POST, PUT or DELETE call with no response value. It's a generic method for REST calls.
     *
     * @param method The http method to be used for this call
     * @param uri The uri which should be called.
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    Observable<RestClientResponse<Void>> request(HttpMethod method, String uri, Action1<RestClientRequest> requestBuilder);

    /**
     * Makes a GET, POST, PUT or DELETE call with a expected response value. It's a generic method for REST calls.
     *
     * @param method The http method to be used for this call
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param requestBuilder The handler to build the request
     * @return A reference to the {@link RestClientRequest}
     */
    <T> Observable<RestClientResponse<T>> request(HttpMethod method, String uri, Class<T> responseClass, Action1<RestClientRequest> requestBuilder);
}
