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
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * @author marcus
 * @since 1.0.0
 */
public class RestClientOptions extends HttpClientOptions {

    private static final Integer DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS = 0;
    private MultiMap globalHeaders = new CaseInsensitiveHeaders();
    private int globalRequestTimeoutInMillis = DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS;

    public RestClientOptions() {
        globalHeaders = new CaseInsensitiveHeaders();
    }

    public RestClientOptions(final HttpClientOptions other) {
        super(other);
    }

    public RestClientOptions(final RestClientOptions other) {
        super(other);
        globalHeaders = new CaseInsensitiveHeaders().addAll(other.getGlobalHeaders());
        globalRequestTimeoutInMillis = other.getGlobalRequestTimeoutInMillis();

    }

    public RestClientOptions(final JsonObject json) {
        super(json);
        globalHeaders = new CaseInsensitiveHeaders();
        globalRequestTimeoutInMillis = json.getInteger("globalRequestTimeoutInMillis", DEFAULT_GLOBAL_REQUEST_TIMEOUT_IN_MILLIS);
    }

    /**
     * Set the request timeout in milliseconds which will be used for every request. It can be overridden on Request level.
     *
     * @param requestTimeoutInMillis The global request timeout in millis.
     * @return a reference to this so multiple method calls can be chained together
     */
    public RestClientOptions setGlobalRequestTimeout(int requestTimeoutInMillis) {
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
     * @param name The name of the header
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
     *
     * @return The request timeout in milliseconds
     */
    public int getGlobalRequestTimeoutInMillis() {
        return globalRequestTimeoutInMillis;
    }

}
