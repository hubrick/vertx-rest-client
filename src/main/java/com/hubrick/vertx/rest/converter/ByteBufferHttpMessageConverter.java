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
package com.hubrick.vertx.rest.converter;

import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

/**
 * Passthrough HttpMessageConverter.
 *
 * @author Emir Dizdarevic
 * @since 2.2.0
 */
public class ByteBufferHttpMessageConverter extends AbstractHttpMessageConverter<ByteBuffer> {

    public ByteBufferHttpMessageConverter() {
        super(new MediaType("application", "octet-stream"), MediaType.ALL);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ByteBuffer.class.isAssignableFrom(clazz);
    }

    @Override
    protected ByteBuffer readInternal(Class<? extends ByteBuffer> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException {
        try {
            return httpInputMessage.getBody().nioBuffer().asReadOnlyBuffer();
        } catch (Exception e) {
            throw new HttpMessageConverterException("Read of http body failed", e);
        }
    }

    @Override
    protected void writeInternal(ByteBuffer object, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException {
        try {
            httpOutputMessage.write(Unpooled.wrappedBuffer(object));
        } catch (Exception e) {
            throw new HttpMessageConverterException("Writing of http body failed", e);
        }
    }
}
