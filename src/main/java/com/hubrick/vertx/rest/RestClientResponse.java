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
package com.hubrick.vertx.rest;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.net.NetSocket;

import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface RestClientResponse<T> {

    /**
     * The HTTP status code of the response
     */
    int statusCode();

    /**
     * The HTTP status message of the response
     */
    String statusMessage();

    /**
     * @return The HTTP headers
     */
    MultiMap headers();

    /**
     * @return The HTTP trailers
     */
    MultiMap trailers();

    /**
     * @return The Set-Cookie headers (including trailers)
     */
    List<String> cookies();

    /**
     * Get a net socket for the underlying connection of this request. USE THIS WITH CAUTION!
     * Writing to the socket directly if you don't know what you're doing can easily break the HTTP protocol
     *
     * One valid use-case for calling this is to receive the {@link NetSocket} after a HTTP CONNECT was issued to the
     * remote peer and it responded with a status code of 200.
     *
     * @return the net socket
     */
    NetSocket netSocket();

    /**
     * At this stage the data mapping happens which can result in a Exception.
     * If no exception handler is set the exception will be thrown.
     *
     * @return Returns the converted body.
     */
    T getBody();

    /**
     * Set an exception handler.
     * Will override the exception handler which was eventually inherited from {@link com.hubrick.vertx.rest.RestClientRequest}.
     */
    void exceptionHandler(Handler<Throwable> exceptionHandler);
}
