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

import com.google.common.collect.ImmutableList;
import com.hubrick.vertx.rest.RestClient;
import com.hubrick.vertx.rest.SslSupport;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

import javax.ws.rs.container.AsyncResponse;
import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class AsyncResponsePropagatingRestClient extends DefaultRestClient {

    private final AsyncResponse asyncResponse;

    public AsyncResponsePropagatingRestClient(AsyncResponse asyncResponse, Vertx vertx, List<HttpMessageConverter> httpMessageConverters) {
        super(vertx, httpMessageConverters);
        this.asyncResponse = asyncResponse;
        super.exceptionHandler(t -> asyncResponse.resume(t));
    }

    @Override
    public final RestClient exceptionHandler(Handler<Throwable> handler) {
        return super.exceptionHandler(handler);
    }

    public final SslSupport setAdditionalExceptionHandler(Handler<Throwable> handler) {
        return super.exceptionHandler(new DelegatingExceptionHandler(ImmutableList.of(
                handler,
                t -> asyncResponse.resume(t)
        )));
    }

    public AsyncResponse getAsyncResponse() {
        return asyncResponse;
    }

    private static class DelegatingExceptionHandler implements Handler<Throwable> {

        private final List<Handler<Throwable>> handlers;

        private DelegatingExceptionHandler(List<Handler<Throwable>> handlers) {
            this.handlers = handlers;
        }

        @Override
        public void handle(Throwable event) {
            for(Handler<Throwable> handler : handlers) {
                handler.handle(event);
            }
        }
    }
}
