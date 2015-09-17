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

import com.hubrick.vertx.rest.RestClientResponse;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.net.NetSocket;

import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class DefaultRxRestClientResponse<T> implements RestClientResponse<T> {

    private final RestClientResponse<T> decorated;

    public DefaultRxRestClientResponse(RestClientResponse<T> decorated) {
        this.decorated = decorated;
    }

    @Override
    public void exceptionHandler(Handler<Throwable> exceptionHandler) {
        throw new UnsupportedOperationException("This method is not supported. Use RxJava's doOnError method to handle exceptions");
    }

    @Override
    public int statusCode() {
        return decorated.statusCode();
    }

    @Override
    public String statusMessage() {
        return decorated.statusMessage();
    }

    @Override
    public MultiMap headers() {
        return decorated.headers();
    }

    @Override
    public MultiMap trailers() {
        return decorated.trailers();
    }

    @Override
    public List<String> cookies() {
        return decorated.cookies();
    }

    @Override
    public NetSocket netSocket() {
        return decorated.netSocket();
    }

    @Override
    public T getBody() {
        return decorated.getBody();
    }
}
