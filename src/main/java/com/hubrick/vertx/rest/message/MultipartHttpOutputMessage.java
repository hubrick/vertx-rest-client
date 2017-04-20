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

import com.google.common.base.Charsets;
import com.hubrick.vertx.rest.HttpOutputMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class MultipartHttpOutputMessage implements HttpOutputMessage {

    private static final Charset multipartCharset = Charsets.US_ASCII;
    private static final byte[] NEW_LINE = "\r\n".getBytes(multipartCharset);
    private static final byte[] COLON = ":".getBytes(multipartCharset);
    private static final byte[] SPACE = " ".getBytes(multipartCharset);

    private final MultiMap headers = new CaseInsensitiveHeaders();
    private final CompositeByteBuf byteBuf = Unpooled.compositeBuffer(32);
    private boolean headersWritten = false;

    @Override
    public MultiMap getHeaders() {
        return headers;
    }

    @Override
    public void write(ByteBuf data) throws IOException {
        writeHeaders();
        byteBuf.addComponent(data).writerIndex(byteBuf.writerIndex() + data.writerIndex());
    }

    private void writeHeaders() throws IOException {
        if (!this.headersWritten) {
            final ByteBuf headersBuf = Unpooled.buffer(1024);    // Header size guess
            for (String name : headers.names()) {
                byte[] headerName = name.getBytes(multipartCharset);
                for (String headerValueString : headers.getAll(name)) {
                    byte[] headerValue = headerValueString.getBytes(multipartCharset);
                    headersBuf.writeBytes(headerName);
                    headersBuf.writeBytes(COLON);
                    headersBuf.writeBytes(SPACE);
                    headersBuf.writeBytes(headerValue);
                    headersBuf.writeBytes(NEW_LINE);
                }
            }
            headersBuf.writeBytes(NEW_LINE);
            byteBuf.addComponent(headersBuf).writerIndex(byteBuf.writerIndex() + headersBuf.writerIndex());
            this.headersWritten = true;
        }
    }

    @Override
    public ByteBuf getBody() {
        return Unpooled.unmodifiableBuffer(byteBuf);
    }

    public void putAllHeaders(MultiMap headers) {
        for (Map.Entry<String, String> header : headers) {
            this.headers.add(header.getKey(), header.getValue());
        }
    }
}
