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
import io.vertx.core.MultiMap;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class BufferedHttpInputMessage implements HttpInputMessage {

    private final byte[] data;
    private final MultiMap headers;

    public BufferedHttpInputMessage(byte[] data, MultiMap headers) {
        this.data = data;
        this.headers = headers;
    }

    @Override
    public byte[] getBody() {
        return data;
    }

    @Override
    public MultiMap getHeaders() {
        return headers;
    }
}
