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
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

    /**
     * Logger available to subclasses
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private List<MediaType> supportedMediaTypes = Collections.emptyList();


    /**
     * Construct an {@code AbstractHttpMessageConverter} with multiple supported media type.
     *
     * @param supportedMediaTypes the supported media types
     */
    protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
        checkNotNull(supportedMediaTypes, "supportedMediaTypes must not be null");
        checkArgument(supportedMediaTypes.length > 0, "supportedMediaTypes must not be empty");

        this.supportedMediaTypes = new ArrayList<>(Arrays.asList(supportedMediaTypes));
    }

    @Override
    public T read(Class<? extends T> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException {
        return readInternal(clazz, httpInputMessage);
    }

    @Override
    public void write(T object, MediaType contentType, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException {
        try {
            final MultiMap headers = httpOutputMessage.getHeaders();
            if (headers.get(HttpHeaders.CONTENT_TYPE) == null) {
                MediaType contentTypeToUse = contentType;
                if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
                    contentTypeToUse = getDefaultContentType(object);
                }
                if (contentTypeToUse != null) {
                    headers.set(HttpHeaders.CONTENT_TYPE, contentTypeToUse.toString());
                }
            }

            writeInternal(object, httpOutputMessage);
        } catch (HttpMessageConverterException e) {
            throw e;
        } catch (Throwable t) {
            throw new HttpMessageConverterException("Error writing object", t);
        }
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(this.supportedMediaTypes);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return supports(clazz) && canRead(mediaType);
    }

    /**
     * Returns true if any of the supported media types
     * include the given media type.
     *
     * @param mediaType the media type to read, can be {@code null} if not specified.
     *                  Typically the value of a {@code Content-Type} header.
     * @return {@code true} if the supported media types include the media type,
     * or if the media type is {@code null}
     */
    protected boolean canRead(MediaType mediaType) {
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
        return supports(clazz) && canWrite(mediaType);
    }

    /**
     * Returns {@code true} if the given media type includes any of the
     * supported media types.
     *
     * @param mediaType the media type to write, can be {@code null} if not specified.
     *                  Typically the value of an {@code Accept} header.
     * @return {@code true} if the supported media types are compatible with the media type,
     * or if the media type is {@code null}
     */
    protected boolean canWrite(MediaType mediaType) {
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

    /**
     * Returns the default content type for the given type. Called when {@link #write}
     * is invoked without a specified content type parameter.
     * <p>By default, this returns the first element of the
     * supportedMediaTypes property, if any.
     * Can be overridden in subclasses.
     *
     * @param t the type to return the content type for
     * @return the content type, or {@code null} if not known
     */
    protected MediaType getDefaultContentType(T t) throws IOException {
        List<MediaType> mediaTypes = getSupportedMediaTypes();
        return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
    }

    /**
     * Indicates whether the given class is supported by this converter.
     *
     * @param clazz the class to test for support
     * @return {@code true} if supported; {@code false} otherwise
     */
    protected abstract boolean supports(Class<?> clazz);

    /**
     * Abstract template method that reads the actual object. Invoked from {@link #read}.
     *
     * @param clazz the type of object to return
     * @return callback handler when the object has been converted
     * @throws com.hubrick.vertx.rest.exception.HttpMessageConverterException in case of conversion errors
     */
    protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException;

    /**
     * Abstract template method that writes the actual body. Invoked from {@link #write}.
     *
     * @param object the object to write to the output message
     * @throws HttpMessageConverterException in case of conversion errors
     */
    protected abstract void writeInternal(T object, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException;
}
