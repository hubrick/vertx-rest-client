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

/**
 * An REST client that maintains a pool of connections to a specific host, at a specific port. The client supports
 * pipelining of requests.<p>
 * If an instance is instantiated from an event loop then the handlers
 * of the instance will always be called on that same event loop.
 * If an instance is instantiated from some other arbitrary Java thread (i.e. when running embedded) then
 * and event loop will be assigned to the instance and used when any of its handlers
 * are called.<p>
 * Instances of RestClient are thread-safe.
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface RestClient extends SslSupport<RestClient> {

    /**
     * Set the port that the client will attempt to connect to the server on to {@code port}. The default value is
     * {@code 80}
     *
     * @param port The port
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setPort(int port);

    /**
     * Set the host that the client will attempt to connect to the server on to {@code host}. The default value is
     * {@code localhost}
     *
     * @param host The host name
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setHost(String host);

    /**
     * Set the TCP keepAlive setting for connections created by this instance to {@code keepAlive}.
     *
     * @param keepAlive Keep alive?
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setTCPKeepAlive(boolean keepAlive);

    /**
     * If {@code tcpNoDelay} is set to {@code true} then <a href="http://en.wikipedia.org/wiki/Nagle's_algorithm">Nagle's algorithm</a>
     * will turned <b>off</b> for the TCP connections created by this instance.
     *
     * @param tcpNoDelay TCP no delay?
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setTCPNoDelay(boolean tcpNoDelay);

    /**
     * Set the TCP soLinger setting for connections created by this instance to {@code linger}.
     * Using a negative value will disable soLinger.
     *
     * @param linger The linger value
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setSoLinger(int linger);

    /**
     * Set if vertx should use pooled buffers for performance reasons. Doing so will give the best throughput but
     * may need a bit higher memory footprint.
     *
     * @param pooledBuffers Pooled buffers?
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setUsePooledBuffers(boolean pooledBuffers);

    /**
     * If {@code keepAlive} is {@code true} then the connection will be returned to the pool after the request has ended (if
     * {@code pipelining} is {@code true}), or after both request and response have ended (if {@code pipelining} is
     * {@code false}). In this manner, many HTTP requests can reuse the same HTTP connection.
     * Keep alive connections will not be closed until the {@link #close() close()} method is invoked.<p>
     * If {@code keepAlive} is {@code false} then a new connection will be created for each request and it won't ever go in the pool,
     * the connection will get closed after the response has been received. Even with no keep alive,
     * the client will not allow more than {@link #getMaxPoolSize()} connections to be created at any one time. <p>
     *
     * @param keepAlive Keep alive?
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setKeepAlive(boolean keepAlive);

    /**
     * Set the maximum waiter queue size<p>
     * The client will keep up to {@code maxWaiterQueueSize} requests in an internal waiting queue<p>
     *
     * @param maxPoolSize The maximum pool size
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setMaxPoolSize(int maxPoolSize);

    /**
     * Set an exception handler. This exception handler will be inherited by the
     * {@link com.hubrick.vertx.rest.RestClientRequest} and {@link com.hubrick.vertx.rest.RestClientResponse}.
     * On each level he can we overridden.
     *
     * @param handler The exception handler
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient exceptionHandler(Handler<Throwable> handler);

    /**
     * If {@code pipelining} is {@code true} and {@code keepAlive} is also {@code true} then, after the request has ended the
     * connection will be returned to the pool where it can be used by another request. In this manner, many HTTP requests can
     * be pipelined over an HTTP connection. If {@code pipelining} is {@code false} and {@code keepAlive} is {@code true}, then
     * the connection will be returned to the pool when both request and response have ended. If {@code keepAlive} is false,
     * then pipelining value is ignored.
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setPipelining(boolean pipelining);

    /**
     * If {@code verifyHost} is {@code true}, then the client will try to validate the remote server's certificate
     * hostname against the requested host. Should default to 'true'.
     * This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)} has been set to {@code true}.
     *
     * @param verifyHost Verify host?
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setVerifyHost(boolean verifyHost);

    /**
     * Set the connect timeout in milliseconds.
     *
     * @param timeout The connect timeout in millis
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setConnectTimeout(int timeout);

    /**
     * Set the request timeout in milliseconds which will be used for every request. It can be overridden on Request level.
     *
     * @param timeout The global request timeout in millis.
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setGlobalRequestTimeout(int timeout);

    /**
     * Set if the {@link com.hubrick.vertx.rest.RestClient} should try to use compression.
     *
     * @param tryUseCompression Try to use compression?
     * @return a reference to this so multiple method calls can be chained together
     */
    RestClient setTryUseCompression(boolean tryUseCompression);

    /**
     * Set the maximum waiter queue size<p>
     * The client will keep up to {@code maxWaiterQueueSize} requests in an internal waiting queue<p>
     *
     * @param maxWaiterQueueSize The max waiter queue size
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setMaxWaiterQueueSize(int maxWaiterQueueSize);

    /**
     * Set the maximum outstanding request size for every connection<p>
     * The connection will keep up to {@code connectionMaxOutstandingRequestCount} outstanding requests<p>
     *
     * @param connectionMaxOutstandingRequestCount Set the maximum outstanding request size for every connection
     * @return A reference to this, so multiple invocations can be chained together.
     */
    RestClient setConnectionMaxOutstandingRequestCount(int connectionMaxOutstandingRequestCount);

    /**
     * Close the HTTP client. This will cause any pooled HTTP connections to be closed.
     */
    void close();

    /**
     * @return the maximum number of connections in the pool
     */
    int getMaxPoolSize();

    /**
     * @return the maximum number of waiting requests
     */
    int getMaxWaiterQueueSize();

    /**
     * @return true if Nagle's algorithm is disabled.
     */
    boolean isTCPNoDelay();
    /**
     *
     * @return true if TCP keep alive is enabled
     */
    boolean isTCPKeepAlive();

    /**
     *
     * @return the value of TCP so linger
     */
    int getSoLinger();

    /**
     * @return {@code true} if pooled buffers are used
     */
    boolean isUsePooledBuffers();

    /**
     *
     * @return Is the client keep alive?
     */
    boolean isKeepAlive();

    /**
     *
     * @return Is enabled HTTP pipelining?
     */
    boolean isPipelining();

    /**
     *
     * @return The port
     */
    int getPort();

    /**
     *
     * @return The host
     */
    String getHost();

    /**
     *
     * @return true if this client will validate the remote server's certificate hostname against the requested host
     */
    boolean isVerifyHost();

    /**
     *
     * @return The connect timeout in milliseconds
     */
    int getConnectTimeout();

    /**
     *
     * @return The request timeout in milliseconds
     */
    int getGlobalRequestTimeoutInMillis();

    /**
     * @return {@code true} if the {@link com.hubrick.vertx.rest.RestClient} should try to use compression.
     */
    boolean getTryUseCompression();

    /**
     * Makes a GET call with no response value.
     *
     * @param uri The uri which should be called.
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    RestClientRequest get(String uri, Handler<RestClientResponse<Void>> responseHandler);

    /**
     * Makes a GET call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    <T> RestClientRequest get(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler);

    /**
     * Makes a POST call with no response value.
     *
     * @param uri The uri which should be called.
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    RestClientRequest post(String uri, Handler<RestClientResponse<Void>> responseHandler);

    /**
     * Makes a POST call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    <T> RestClientRequest post(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler);

    /**
     * Makes a PUT call with no response value.
     *
     * @param uri The uri which should be called.
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    RestClientRequest put(String uri, Handler<RestClientResponse<Void>> responseHandler);

    /**
     * Makes a PUT call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    <T> RestClientRequest put(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler);

    /**
     * Makes a DELETE call with no response value.
     *
     * @param uri The uri which should be called.
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    RestClientRequest delete(String uri, Handler<RestClientResponse<Void>> responseHandler);

    /**
     * Makes a DELETE call with a expected response value.
     *
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    <T> RestClientRequest delete(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler);

    /**
     * Makes a GET, POST, PUT or DELETE call with no response value. It's a generic method for REST calls.
     *
     * @param method The http method to be used for this call
     * @param uri The uri which should be called.
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    RestClientRequest request(HttpMethod method, String uri, Handler<RestClientResponse<Void>> responseHandler);

    /**
     * Makes a GET, POST, PUT or DELETE call with a expected response value. It's a generic method for REST calls.
     *
     * @param method The http method to be used for this call
     * @param uri The uri which should be called.
     * @param responseClass The class which represents the response
     * @param responseHandler The handler for the response callback
     * @return A reference to the {@link com.hubrick.vertx.rest.RestClientRequest}
     */
    <T> RestClientRequest request(HttpMethod method, String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler);
}
