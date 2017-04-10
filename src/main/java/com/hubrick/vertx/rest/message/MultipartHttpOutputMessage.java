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
import java.util.Map;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class MultipartHttpOutputMessage implements HttpOutputMessage {

    private MultiMap headers = new CaseInsensitiveHeaders();
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

    @Override
    public ByteBuf getBody() {
        byteBuf.resetReaderIndex();
        return Unpooled.unmodifiableBuffer(byteBuf);
    }

    private void writeHeaders() throws IOException {
        final ByteBuf headersBuf = Unpooled.directBuffer();
        if (!this.headersWritten) {
            for (String name : headers.names()) {
                byte[] headerName = name.getBytes(Charsets.US_ASCII);
                for (String headerValueString : headers.getAll(name)) {
                    byte[] headerValue = headerValueString.getBytes(Charsets.US_ASCII);
                    headersBuf.writeBytes(headerName);
                    headersBuf.writeChar(':');
                    headersBuf.writeChar(' ');
                    headersBuf.writeBytes(headerValue);
                    writeNewLine(headersBuf);
                }
            }
            writeNewLine(headersBuf);
            byteBuf.addComponent(headersBuf).writerIndex(byteBuf.writerIndex() + headersBuf.writerIndex());
            this.headersWritten = true;
        }
    }

    private static void writeNewLine(ByteBuf byteBuf) throws IOException {
        byteBuf.writeChar('\r');
        byteBuf.writeChar('\n');
    }

    public void putAllHeaders(MultiMap headers) {
        for (Map.Entry<String, String> header : headers) {
            this.headers.add(header.getKey(), header.getValue());
        }
    }
}
