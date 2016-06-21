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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author marcus
 * @since 1.0.0
 */
public class RestClientOptions extends HttpClientOptions {

    private static final int DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS = 0;

    private Optional<RequestCacheOptions> globalRequestCacheOptions = Optional.empty();
    private int globalRequestTimeoutInMillis = DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS;
    private MultiMap globalHeaders = new CaseInsensitiveHeaders();

    public RestClientOptions() {
        globalHeaders = new CaseInsensitiveHeaders();
    }

    public RestClientOptions(final HttpClientOptions other) {
        super(other);
    }

    public RestClientOptions(final RestClientOptions other) {
        super(other);
        globalRequestCacheOptions = other.globalRequestCacheOptions;
        globalHeaders = new CaseInsensitiveHeaders().addAll(other.getGlobalHeaders());
        globalRequestTimeoutInMillis = other.getGlobalRequestTimeoutInMillis();

    }

    public RestClientOptions(final JsonObject json) {
        super(json);
        final JsonObject jsonObjectGlobalRequestCacheOptions = json.getJsonObject("globalRequestCacheOptions");
        if (jsonObjectGlobalRequestCacheOptions != null) {
            final RequestCacheOptions requestCacheOptions = new RequestCacheOptions();
            final Integer ttlInMillis = jsonObjectGlobalRequestCacheOptions.getInteger("ttlInMillis");
            final Boolean evictBefore = jsonObjectGlobalRequestCacheOptions.getBoolean("evictBefore");
            if (jsonObjectGlobalRequestCacheOptions.getJsonArray("cachedStatusCodes") != null) {
                final Set<Integer> cachedStatusCodes = jsonObjectGlobalRequestCacheOptions.getJsonArray("cachedStatusCodes")
                        .stream()
                        .map(e -> (Integer) e)
                        .collect(Collectors.toSet());
                requestCacheOptions.withCachedStatusCodes(cachedStatusCodes);
            }

            if (ttlInMillis != null) {
                requestCacheOptions.withTtlInMillis(ttlInMillis);
            }
            if (evictBefore != null) {
                requestCacheOptions.withEvictBefore(evictBefore);
            }
            globalRequestCacheOptions = Optional.of(requestCacheOptions);
        }
        globalHeaders = new CaseInsensitiveHeaders();
        globalRequestTimeoutInMillis = json.getInteger("globalRequestTimeoutInMillis", DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS);
    }

    /**
     * @return The request timeout in milliseconds
     */
    public int getGlobalRequestTimeoutInMillis() {
        return globalRequestTimeoutInMillis;
    }

    /**
     * Set the request timeout in milliseconds which will be used for every request. It can be overridden on Request level.
     *
     * @param requestTimeoutInMillis The global request timeout in millis.
     * @return a reference to this so multiple method calls can be chained together
     */
    public RestClientOptions setGlobalRequestTimeout(int requestTimeoutInMillis) {
        checkArgument(requestTimeoutInMillis > 0, "ttlInMillis must be greater then 0");

        this.globalRequestTimeoutInMillis = requestTimeoutInMillis;
        return this;
    }

    /**
     * Set global headers which will be appended to every HTTP request.
     * The headers defined per request will override this headers.
     *
     * @param headers The headers to add
     * @return a reference to this so multiple method calls can be chained together
     */
    public RestClientOptions putGlobalHeaders(MultiMap headers) {
        for (Map.Entry<String, String> header : headers) {
            globalHeaders.add(header.getKey(), header.getValue());
        }

        return this;
    }

    /**
     * Add a global header which will be appended to every HTTP request.
     * The headers defined per request will override this headers.
     *
     * @param name  The name of the header
     * @param value The value of the header
     * @return a reference to this so multiple method calls can be chained together
     */
    public RestClientOptions putGlobalHeader(String name, String value) {
        globalHeaders.add(name, value);
        return this;
    }

    public MultiMap getGlobalHeaders() {
        return globalHeaders;
    }

    /**
     * Sets the cache config and enables it for all request. Default is Optional.empty().
     * It can be overridden on the request level.
     * 
     * The cache key is generated from the url, the headers and the request body.
     *
     * @param requestCacheOptions The request cache config
     * @return a reference to this so multiple method calls can be chained together
     */
    public RestClientOptions setGlobalRequestCacheOptions(Optional<RequestCacheOptions> requestCacheOptions) {
        checkNotNull(requestCacheOptions, "requestCacheOptions must not be null");
        globalRequestCacheOptions = requestCacheOptions;
        return this;
    }

    public Optional<RequestCacheOptions> getGlobalRequestCacheOptions() {
        return globalRequestCacheOptions;
    }

    @Override
    public RestClientOptions setSendBufferSize(int sendBufferSize) {
        super.setSendBufferSize(sendBufferSize);
        return this;
    }

    @Override
    public RestClientOptions setReceiveBufferSize(int receiveBufferSize) {
        super.setReceiveBufferSize(receiveBufferSize);
        return this;
    }

    @Override
    public RestClientOptions setReuseAddress(boolean reuseAddress) {
        super.setReuseAddress(reuseAddress);
        return this;
    }

    @Override
    public RestClientOptions setTrafficClass(int trafficClass) {
        super.setTrafficClass(trafficClass);
        return this;
    }

    @Override
    public RestClientOptions setTcpNoDelay(boolean tcpNoDelay) {
        super.setTcpNoDelay(tcpNoDelay);
        return this;
    }

    @Override
    public RestClientOptions setTcpKeepAlive(boolean tcpKeepAlive) {
        super.setTcpKeepAlive(tcpKeepAlive);
        return this;
    }

    @Override
    public RestClientOptions setSoLinger(int soLinger) {
        super.setSoLinger(soLinger);
        return this;
    }

    @Override
    public RestClientOptions setUsePooledBuffers(boolean usePooledBuffers) {
        super.setUsePooledBuffers(usePooledBuffers);
        return this;
    }

    @Override
    public RestClientOptions setIdleTimeout(int idleTimeout) {
        super.setIdleTimeout(idleTimeout);
        return this;
    }

    @Override
    public RestClientOptions setSsl(boolean ssl) {
        super.setSsl(ssl);
        return this;
    }

    @Override
    public RestClientOptions setKeyStoreOptions(JksOptions options) {
        super.setKeyStoreOptions(options);
        return this;
    }

    @Override
    public RestClientOptions setPfxKeyCertOptions(PfxOptions options) {
        super.setPfxKeyCertOptions(options);
        return this;
    }

    @Override
    public RestClientOptions setPemKeyCertOptions(PemKeyCertOptions options) {
        super.setPemKeyCertOptions(options);
        return this;
    }

    @Override
    public RestClientOptions setTrustStoreOptions(JksOptions options) {
        super.setTrustStoreOptions(options);
        return this;
    }

    @Override
    public RestClientOptions setPfxTrustOptions(PfxOptions options) {
        super.setPfxTrustOptions(options);
        return this;
    }

    @Override
    public RestClientOptions setPemTrustOptions(PemTrustOptions options) {
        super.setPemTrustOptions(options);
        return this;
    }

    @Override
    public RestClientOptions addEnabledCipherSuite(String suite) {
        super.addEnabledCipherSuite(suite);
        return this;
    }

    @Override
    public RestClientOptions addCrlPath(String crlPath) throws NullPointerException {
        super.addCrlPath(crlPath);
        return this;
    }

    @Override
    public RestClientOptions addCrlValue(Buffer crlValue) throws NullPointerException {
        super.addCrlValue(crlValue);
        return this;
    }

    @Override
    public RestClientOptions setConnectTimeout(int connectTimeout) {
        super.setConnectTimeout(connectTimeout);
        return this;
    }

    @Override
    public RestClientOptions setTrustAll(boolean trustAll) {
        super.setTrustAll(trustAll);
        return this;
    }

    @Override
    public RestClientOptions setMaxPoolSize(int maxPoolSize) {
        super.setMaxPoolSize(maxPoolSize);
        return this;
    }

    @Override
    public RestClientOptions setKeepAlive(boolean keepAlive) {
        super.setKeepAlive(keepAlive);
        return this;
    }

    @Override
    public RestClientOptions setPipelining(boolean pipelining) {
        super.setPipelining(pipelining);
        return this;
    }

    @Override
    public RestClientOptions setVerifyHost(boolean verifyHost) {
        super.setVerifyHost(verifyHost);
        return this;
    }

    @Override
    public RestClientOptions setTryUseCompression(boolean tryUseCompression) {
        super.setTryUseCompression(tryUseCompression);
        return this;
    }

    @Override
    public RestClientOptions setMaxWebsocketFrameSize(int maxWebsocketFrameSize) {
        super.setMaxWebsocketFrameSize(maxWebsocketFrameSize);
        return this;
    }

    @Override
    public RestClientOptions setDefaultHost(String defaultHost) {
        super.setDefaultHost(defaultHost);
        return this;
    }

    @Override
    public RestClientOptions setDefaultPort(int defaultPort) {
        super.setDefaultPort(defaultPort);
        return this;
    }
}
