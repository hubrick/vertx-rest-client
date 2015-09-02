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
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpHeaders;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class FormHttpMessageConverter implements HttpMessageConverter<Multimap<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(FormHttpMessageConverter.class);

    private static final Charset charset = Charsets.UTF_8;
    private static final List<MediaType> supportedMediaTypes = ImmutableList.of(MediaType.APPLICATION_FORM_URLENCODED);

    @Override
    public Multimap<String, Object> read(Class<? extends Multimap<String, Object>> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException {
        final MediaType mediaType = MediaType.parseMediaType(httpInputMessage.getHeaders().get(HttpHeaders.CONTENT_TYPE));
        Charset charset = (mediaType.getCharSet() != null ? mediaType.getCharSet() : this.charset);
        String body = new String(httpInputMessage.getBody(), charset);

        try {
            String[] pairs = FluentIterable.from(Splitter.on("&").split(body)).toArray(String.class);
            Multimap<String, Object> result = HashMultimap.create();
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx == -1) {
                    result.put(URLDecoder.decode(pair, charset.name()), null);
                } else {
                    String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
                    String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                    result.put(name, value);
                }
            }

            return result;
        } catch (UnsupportedEncodingException e) {
            throw new HttpMessageConverterException(e);
        }
    }

    @Override
    public void write(Multimap<String, Object> object, MediaType contentType, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException {
        try {
            writeForm(object, contentType, httpOutputMessage);
        } catch (IOException e) {
            throw new HttpMessageConverterException(e);
        }
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        if (!Multimap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            if (supportedMediaType.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!Multimap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null || MediaType.ALL.equals(mediaType)) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            if (supportedMediaType.isCompatibleWith(mediaType)) {
                return true;
            }
        }
        return false;
    }

    private void writeForm(Multimap<String, Object> form, MediaType contentType, HttpOutputMessage httpOutputMessage) throws IOException {

        Charset charset;
        if (contentType != null) {
            httpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType.toString());
            charset = contentType.getCharSet() != null ? contentType.getCharSet() : this.charset;
        } else {
            httpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.toString());
            charset = this.charset;
        }

        StringBuilder builder = new StringBuilder();
        for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
            String name = nameIterator.next();
            for (Iterator<Object> valueIterator = form.get(name).iterator(); valueIterator.hasNext(); ) {
                Object value = valueIterator.next();
                builder.append(URLEncoder.encode(name, charset.name()));
                if (value != null) {
                    builder.append('=');
                    builder.append(URLEncoder.encode(String.valueOf(value), charset.name()));
                    if (valueIterator.hasNext()) {
                        builder.append('&');
                    }
                }
            }
            if (nameIterator.hasNext()) {
                builder.append('&');
            }
        }
        final String payload = builder.toString();
        final byte[] bytes = payload.getBytes(charset.name());
        httpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
        httpOutputMessage.write(bytes);
    }
}
