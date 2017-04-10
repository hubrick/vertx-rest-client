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
package com.hubrick.vertx.rest.message;

import com.hubrick.vertx.rest.HttpInputMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class BufferedHttpInputMessage implements HttpInputMessage {

    private final ByteBuf data;
    private final MultiMap headers;
    private final MultiMap trailers;
    private final String statusMessage;
    private final Integer statusCode;
    private final List<String> cookies;

    public BufferedHttpInputMessage(ByteBuf data, MultiMap headers, MultiMap trailers, String statusMessage, Integer statusCode, List<String> cookies) {
        checkNotNull(data, "data must not be null");
        checkNotNull(headers, "headers must not be null");
        checkNotNull(statusMessage, "statusMessage must not be null");
        checkNotNull(statusCode, "statusCode must not be null");

        this.data = data;
        this.headers = headers;
        this.trailers = trailers == null ? MultiMap.caseInsensitiveMultiMap() : trailers;
        this.statusMessage = statusMessage;
        this.statusCode = statusCode;
        this.cookies = cookies == null ? Collections.emptyList() : cookies;
    }

    @Override
    public ByteBuf getBody() {
        data.resetReaderIndex();
        return Unpooled.unmodifiableBuffer(data);
    }

    @Override
    public MultiMap getHeaders() {
        return headers;
    }

    @Override
    public MultiMap getTrailers() {
        return trailers;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public Integer getStatusCode() {
        return statusCode;
    }

    @Override
    public List<String> getCookies() {
        return cookies;
    }
}
