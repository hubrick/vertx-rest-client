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


import io.vertx.core.Handler;
import io.vertx.core.MultiMap;

import java.util.List;

/**
 * Represents a client-side REST request.<p>
 * Instances are created by an {@link com.hubrick.vertx.rest.RestClient} instance, via one of the methods corresponding to the
 * specific HTTP methods, or the generic {@link com.hubrick.vertx.rest.RestClient#request} method.<p>
 * Once a request has been obtained, headers can be set on it, and data can be written to its body if required. Once
 * you are ready to send the request, the {@link #end()} method should be called.<p>
 * Nothing is actually sent until the request has been internally assigned an HTTP connection. The {@link com.hubrick.vertx.rest.RestClient}
 * instance will return an instance of this class immediately, even if there are no HTTP connections available in the pool. Any requests
 * sent before a connection is assigned will be queued internally and actually sent when an HTTP connection becomes
 * available from the pool.<p>
 * The headers of the request are actually sent either when the {@link #end()} method is called, or, when the first
 * part of the body is written, whichever occurs first.<p>
 * This class supports both chunked and non-chunked HTTP.<p>
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface RestClientRequest<T> {

    /**
     * If chunked is true then the request will be set into HTTP chunked mode
     * @param chunked
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> setChunked(boolean chunked);

    /**
     *
     * @return Is the request chunked?
     */
    boolean isChunked();

    /**
     * @return The HTTP headers
     */
    MultiMap headers();

    /**
     * Put an HTTP header - fluent API
     * @param name The header name
     * @param value The header value
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> putHeader(String name, String value);

    RestClientRequest<T> putHeader(CharSequence name, CharSequence value);

    /**
     * Put an HTTP header - fluent API
     * @param name The header name
     * @param values The header values
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> putHeader(String name, Iterable<String> values);

    RestClientRequest<T> putHeader(CharSequence name, Iterable<CharSequence> values);

    /**
     * Write a object to the request body.
     * NOTE: if using this method you have either to set the request to chunked or
     * to set the Content-Length before ending the request.
     *
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> write(Object requestObject);

    /**
     * If you send an HTTP request with the header {@code Expect} set to the value {@code 100-continue}
     * and the server responds with an interim HTTP response with a status code of {@code 100} and a continue handler
     * has been set using this method, then the {@code handler} will be called.<p>
     * You can then continue to write data to the request body and later end it. This is normally used in conjunction with
     * the {@link #sendHead()} method to force the request header to be written before the request has ended.
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> continueHandler(Handler<Void> handler);

    /**
     * Forces the head of the request to be written before {@link #end()} is called on the request or any data is
     * written to it. This is normally used
     * to implement HTTP 100-continue handling, see {@link #continueHandler(io.vertx.core.Handler)} for more information.
     *
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> sendHead();

    /**
     * Same as {@link #end()} but writes the requestObject to the request body before ending. If the request is not chunked and
     * no other data has been written then the Content-Length header will be automatically set
     */
    void end(Object requestObject);

    /**
     * Ends the request. If no data has been written to the request body, and {@link #sendHead()} has not been called then
     * the actual request won't get written until this method gets called.<p>
     * Once the request has ended, it cannot be used any more, and if keep alive is true the underlying connection will
     * be returned to the {@link com.hubrick.vertx.rest.RestClient} pool so it can be assigned to another request.
     */
    void end();

    /**
     * Set's the amount of time after which if a response is not received TimeoutException()
     * will be sent to the exception handler of this request. Calling this method more than once
     * has the effect of canceling any existing timeout and starting the timeout from scratch.
     *
     * @param timeoutMs The quantity of time in milliseconds.
     * @return A reference to this, so multiple method calls can be chained.
     */
    RestClientRequest<T> setTimeout(long timeoutMs);

    /**
     * Sets the Content-Type of this request.
     *
     * @param contentType The content type to set.
     */
    void setContentType(MediaType contentType);

    /**
     *
     * @return The Content-Type if present otherwise null.
     */
    MediaType getContentType();

    /**
     * Sets a List of MediaTypes for the Accept header. If not set the default value
     * is a list of MediaTypes which is taken from the defined {@link com.hubrick.vertx.rest.converter.HttpMessageConverter}.
     * Basically it means that you accept all types which you are able to convert.
     *
     * @param mediaTypes
     */
    void setAcceptHeader(List<MediaType> mediaTypes);

    /**
     *
     * @return The list of MediaTypes from the Accept header
     */
    List<MediaType> getAcceptHeader();

    /**
     * Set's the credentials for the basic auth.
     *
     * @param userPassCombination This has to be in the following format {@code <user>:<password>}
     */
    void setBasicAuth(String userPassCombination);

    /**
     *
     * @return The basic auth credentials in the following format {@code <user>:<password>} if present otherwise null.
     */
    String getBasicAuth();

    /**
     * Set an exception handler. Will override the exception handler which was eventually inherited from {@link com.hubrick.vertx.rest.RestClient}.
     * This exception handler will be inherited by the {@link com.hubrick.vertx.rest.RestClientResponse}. On each level he can we overridden.
     *
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClientRequest<T> exceptionHandler(Handler<Throwable> exceptionHandler);
}
