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

import com.hubrick.vertx.rest.HttpMethod;
import com.hubrick.vertx.rest.RestClient;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;

import javax.net.ssl.SSLContext;
import java.util.List;

/**
 * The default implementation.
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class DefaultRestClient implements RestClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultRestClient.class);

    private final Vertx vertx;
    private final HttpClient httpClient;
    private final List<HttpMessageConverter> httpMessageConverters;
    private Handler<Throwable> exceptionHandler;
    private int globalRequestTimeoutInMillis = 0;

    public DefaultRestClient(Vertx vertx, List<HttpMessageConverter> httpMessageConverters) {
        this.vertx = vertx;
        this.httpMessageConverters = httpMessageConverters;
        this.httpClient = vertx.createHttpClient();
    }

    @Override
    public RestClient setPort(int port) {
        httpClient.setPort(port);
        return this;
    }

    @Override
    public RestClient setHost(String host) {
        httpClient.setHost(host);
        return this;
    }

    @Override
    public RestClient setTCPKeepAlive(boolean keepAlive) {
        httpClient.setTCPKeepAlive(keepAlive);
        return this;
    }

    @Override
    public RestClient setTCPNoDelay(boolean tcpNoDelay) {
        httpClient.setTCPNoDelay(tcpNoDelay);
        return this;
    }

    @Override
    public RestClient setSoLinger(int linger) {
        return null;
    }

    @Override
    public RestClient setUsePooledBuffers(boolean pooledBuffers) {
        httpClient.setUsePooledBuffers(true);
        return this;
    }

    @Override
    public RestClient setKeepAlive(boolean keepAlive) {
        httpClient.setKeepAlive(keepAlive);
        return this;
    }

    @Override
    public RestClient setMaxPoolSize(int maxPoolSize) {
        httpClient.setMaxPoolSize(maxPoolSize);
        return this;
    }

    @Override
    public RestClient exceptionHandler(Handler<Throwable> handler) {
        this.exceptionHandler = handler;
        httpClient.exceptionHandler(handler);
        return this;
    }

    @Override
    public RestClient setPipelining(boolean pipelining) {
        httpClient.setPipelining(pipelining);
        return this;
    }

    @Override
    public RestClient setVerifyHost(boolean verifyHost) {
        httpClient.setVerifyHost(verifyHost);
        return this;
    }

    @Override
    public RestClient setConnectTimeout(int timeout) {
        httpClient.setConnectTimeout(timeout);
        return this;
    }

    @Override
    public RestClient setGlobalRequestTimeout(int requestTimeoutInMillis) {
        this.globalRequestTimeoutInMillis = requestTimeoutInMillis;
        return this;
    }

    @Override
    public RestClient setTryUseCompression(boolean tryUseCompression) {
        httpClient.setTryUseCompression(tryUseCompression);
        return this;
    }

    @Override
    public RestClient setMaxWaiterQueueSize(int maxWaiterQueueSize) {
        httpClient.setMaxWaiterQueueSize(maxWaiterQueueSize);
        return this;
    }

    @Override
    public RestClient setConnectionMaxOutstandingRequestCount(int connectionMaxOutstandingRequestCount) {
        httpClient.setConnectionMaxOutstandingRequestCount(connectionMaxOutstandingRequestCount);
        return this;
    }

    @Override
    public void close() {
        httpClient.close();
    }

    @Override
    public int getMaxPoolSize() {
        return httpClient.getMaxPoolSize();
    }

    @Override
    public int getMaxWaiterQueueSize() {
        return httpClient.getMaxWaiterQueueSize();
    }

    @Override
    public boolean isTCPNoDelay() {
        return httpClient.isTCPNoDelay();
    }

    @Override
    public boolean isTCPKeepAlive() {
        return httpClient.isTCPKeepAlive();
    }

    @Override
    public int getSoLinger() {
        return httpClient.getSoLinger();
    }

    @Override
    public boolean isUsePooledBuffers() {
        return httpClient.isUsePooledBuffers();
    }

    @Override
    public boolean isKeepAlive() {
        return httpClient.isKeepAlive();
    }

    @Override
    public boolean isPipelining() {
        return httpClient.isPipelining();
    }

    @Override
    public int getPort() {
        return httpClient.getPort();
    }

    @Override
    public String getHost() {
        return httpClient.getHost();
    }

    @Override
    public boolean isVerifyHost() {
        return httpClient.isVerifyHost();
    }

    @Override
    public int getConnectTimeout() {
        return httpClient.getConnectTimeout();
    }

    @Override
    public int getGlobalRequestTimeoutInMillis() {
        return globalRequestTimeoutInMillis;
    }

    @Override
    public boolean getTryUseCompression() {
        return httpClient.getTryUseCompression();
    }

    @Override
    public RestClientRequest<Void> get(String uri, Handler<RestClientResponse<Void>> responseHandler) {
        return get(uri, Void.class, responseHandler);
    }

    @Override
    public <T> RestClientRequest<T> get(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        log.debug("Calling uri: {}", uri);
        return handleRequest(HttpMethod.GET, uri, responseClass, responseHandler);
    }

    @Override
    public RestClientRequest<Void> post(String uri, Handler<RestClientResponse<Void>> responseHandler) {
        return post(uri, Void.class, responseHandler);
    }

    @Override
    public <T> RestClientRequest<T> post(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        log.debug("Calling uri: {}", uri);
        return handleRequest(HttpMethod.POST, uri, responseClass, responseHandler);
    }

    @Override
    public RestClientRequest<Void> put(String uri, Handler<RestClientResponse<Void>> responseHandler) {
        return put(uri, Void.class, responseHandler);
    }

    @Override
    public <T> RestClientRequest<T> put(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        log.debug("Calling uri: {}", uri);
        return handleRequest(HttpMethod.PUT, uri, responseClass, responseHandler);
    }

    @Override
    public RestClientRequest<Void> delete(String uri, Handler<RestClientResponse<Void>> responseHandler) {
        return delete(uri, Void.class, responseHandler);
    }

    @Override
    public <T> RestClientRequest<T> delete(String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        log.debug("Calling uri: {}", uri);
        return handleRequest(HttpMethod.DELETE, uri, responseClass, responseHandler);
    }

    @Override
    public RestClientRequest<Void> request(HttpMethod method, String uri, Handler<RestClientResponse<Void>> responseHandler) {
        return request(method, uri, Void.class, responseHandler);
    }

    @Override
    public <T> RestClientRequest<T> request(HttpMethod method, String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        log.debug("Calling uri: {}", uri);
        return handleRequest(method, uri, responseClass, responseHandler);
    }

    private <T> DefaultRestClientRequest<T> handleRequest(HttpMethod method, String uri, Class<T> responseClass, Handler<RestClientResponse<T>> responseHandler) {
        return new DefaultRestClientRequest(
                httpClient,
                httpMessageConverters,
                method,
                uri,
                responseClass,
                responseHandler,
                globalRequestTimeoutInMillis,
                exceptionHandler
        );
    }

    @Override
    public RestClient setSSL(boolean ssl) {
        httpClient.setSSL(ssl);
        return this;
    }

    @Override
    public boolean isSSL() {
        return httpClient.isSSL();
    }

    @Override
    public RestClient setTrustAll(boolean trustAll) {
        httpClient.setTrustAll(trustAll);
        return this;
    }

    @Override
    public boolean isTrustAll() {
        return httpClient.isTrustAll();
    }

    @Override
    public RestClient setSSLContext(SSLContext sslContext) {
        httpClient.setSSLContext(sslContext);
        return this;
    }

    @Override
    public RestClient setKeyStorePath(String path) {
        httpClient.setKeyStorePath(path);
        return this;
    }

    @Override
    public String getKeyStorePath() {
        return httpClient.getKeyStorePath();
    }

    @Override
    public RestClient setKeyStorePassword(String pwd) {
        httpClient.setKeyStorePassword(pwd);
        return this;
    }

    @Override
    public String getKeyStorePassword() {
        return httpClient.getKeyStorePassword();
    }

    @Override
    public RestClient setTrustStorePath(String path) {
        httpClient.setTrustStorePath(path);
        return this;
    }

    @Override
    public String getTrustStorePath() {
        return httpClient.getTrustStorePath();
    }

    @Override
    public RestClient setTrustStorePassword(String pwd) {
        httpClient.setTrustStorePassword(pwd);
        return this;
    }

    @Override
    public String getTrustStorePassword() {
        return httpClient.getTrustStorePassword();
    }
}
