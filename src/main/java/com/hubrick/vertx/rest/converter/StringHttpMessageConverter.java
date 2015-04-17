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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpHeaders;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

    private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;

    private final Charset defaultCharset;
    private final List<Charset> availableCharsets;
    private boolean writeAcceptCharset = true;

    public StringHttpMessageConverter() {
        this(DEFAULT_CHARSET);
    }

    public StringHttpMessageConverter(Charset defaultCharset) {
        super(new MediaType("text", "plain", defaultCharset), MediaType.ALL);
        this.defaultCharset = defaultCharset;
        this.availableCharsets = new ArrayList<Charset>(Charset.availableCharsets().values());
    }

    /**
     * Indicates whether the {@code Accept-Charset} should be written to any outgoing request.
     * <p>Default is {@code true}.
     */
    public void setWriteAcceptCharset(boolean writeAcceptCharset) {
        this.writeAcceptCharset = writeAcceptCharset;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return String.class.equals(clazz);
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, byte[] buffer, MultiMap responseHeaders) throws HttpMessageConverterException {
        final Charset charset = getContentTypeCharset(MediaType.parseMediaType(responseHeaders.get(HttpHeaders.CONTENT_TYPE)));
        return new String(buffer, charset);
    }

    @Override
    protected byte[] writeInternal(String object, MultiMap requestHeaders) throws HttpMessageConverterException {
        if (this.writeAcceptCharset) {
            requestHeaders.set(HttpHeaders.ACCEPT_CHARSET, Joiner.on(",").join(getAcceptedCharsets()));
        }
        final Charset charset = getContentTypeCharset(MediaType.parseMediaType(requestHeaders.get(HttpHeaders.CONTENT_TYPE)));
        return object.getBytes(charset);
    }

    /**
     * Return the list of supported {@link Charset}s.
     * <p>By default, returns {@link Charset#availableCharsets()}.
     * Can be overridden in subclasses.
     * @return the list of accepted charsets
     */
    protected List<Charset> getAcceptedCharsets() {
        return this.availableCharsets;
    }

    private Charset getContentTypeCharset(MediaType contentType) {
        if (contentType != null && contentType.getCharSet() != null) {
            return contentType.getCharSet();
        }
        else {
            return this.defaultCharset;
        }
    }
}
