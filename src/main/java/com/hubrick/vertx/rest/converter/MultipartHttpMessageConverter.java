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
import com.google.common.collect.Multimap;
import com.hubrick.vertx.rest.HttpInputMessage;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.converter.model.Part;
import com.hubrick.vertx.rest.exception.HttpMessageConverterException;
import com.hubrick.vertx.rest.message.MultipartHttpOutputMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class MultipartHttpMessageConverter implements HttpMessageConverter<Multimap<String, Part>> {

    private static final Logger log = LoggerFactory.getLogger(MultipartHttpMessageConverter.class);

    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final byte[] BOUNDARY_CHARS =
            new byte[]{'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                    'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
                    'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                    'V', 'W', 'X', 'Y', 'Z'};

    private static final Charset charset = Charsets.UTF_8;
    private Charset multipartCharset = Charsets.US_ASCII;
    private final Random random = new Random();

    private final List<MediaType> supportedMediaTypes = new ArrayList<>();
    private List<HttpMessageConverter> partConverters = new ArrayList<>();

    public MultipartHttpMessageConverter() {
        this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);

        final StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
        stringHttpMessageConverter.setWriteAcceptCharset(false);
        this.partConverters.add(new ByteArrayHttpMessageConverter());
        this.partConverters.add(new ByteBufHttpMessageConverter());
        this.partConverters.add(new ByteBufferHttpMessageConverter());
        this.partConverters.add(stringHttpMessageConverter);
    }

    /**
     * Set the character set to use when writing multipart data to encode file
     * names. Encoding is based on the encoded-word syntax defined in RFC 2047
     * and relies on {@code MimeUtility} from "javax.mail".
     * <p>If not set file names will be encoded as US-ASCII.
     *
     * @param multipartCharset the charset to use
     * @see <a href="http://en.wikipedia.org/wiki/MIME#Encoded-Word">Encoded-Word</a>
     * @since 4.1.1
     */
    public void setMultipartCharset(Charset multipartCharset) {
        checkNotNull(multipartCharset, "multipartCharset must not be null");
        this.multipartCharset = multipartCharset;
    }

    /**
     * Set the message body converters to use. These converters are used to
     * convert objects to MIME parts.
     */
    public void setPartConverters(List<HttpMessageConverter> partConverters) {
        checkNotNull(partConverters, "'partConverters' must not be null");
        checkArgument(!partConverters.isEmpty(), "'partConverters' must not be empty");
        this.partConverters = partConverters;
    }

    /**
     * Add a message body converter. Such a converter is used to convert objects
     * to MIME parts.
     */
    public void addPartConverter(HttpMessageConverter partConverter) {
        checkNotNull(partConverters, "'partConverters' must not be null");
        checkArgument(!partConverters.isEmpty(), "'partConverters' must not be empty");
        this.partConverters.add(partConverter);
    }

    @Override
    public Multimap<String, Part> read(Class<? extends Multimap<String, Part>> clazz, HttpInputMessage httpInputMessage) throws HttpMessageConverterException {
        throw new UnsupportedOperationException("The multipart read operation is currently not supported.");
    }

    @Override
    public void write(Multimap<String, Part> object, MediaType contentType, HttpOutputMessage httpOutputMessage) throws HttpMessageConverterException {
        try {
            if (isMultipart(object, contentType)) {
                writeMultipart(object, httpOutputMessage);
            } else {
                throw new HttpMessageConverterException("Unable to write multipart data. Content-Type wrong or Multimap contains wrong data.");
            }
        } catch (IOException e) {
            throw new HttpMessageConverterException(e);
        }
    }

    private boolean isMultipart(Multimap<String, Part> map, MediaType contentType) {
        if (contentType != null) {
            if (!MediaType.MULTIPART_FORM_DATA.includes(contentType)) {
                return false;
            }
        }

        for (Map.Entry<String, Part> value : map.entries()) {
            if (!(value.getValue() instanceof Part)) {
                return false;
            }
        }

        return true;
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

    private void writeMultipart(Multimap<String, Part> parts, HttpOutputMessage httpOutputMessage) throws IOException {
        final String boundary = generateMultipartBoundary();
        final Map<String, String> parameters = Collections.singletonMap("boundary", boundary);

        final MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
        httpOutputMessage.getHeaders().set(HttpHeaders.CONTENT_TYPE, contentType.toString());

        writeParts(httpOutputMessage, parts, boundary);
        writeEnd(httpOutputMessage, boundary);
    }

    private void writeParts(HttpOutputMessage httpOutputMessage, Multimap<String, Part> parts, String boundary) throws IOException {
        for (Map.Entry<String, Part> entry : parts.entries()) {
            final CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
            String name = entry.getKey();
            if (entry.getValue() != null) {
                final ByteBuf boundaryByteBub = writeBoundary(boundary);
                final ByteBuf partByteBuf = writePart(name, entry.getValue());
                final ByteBuf newLineByteBuf = writeNewLine();
                compositeByteBuf.addComponent(boundaryByteBub).writerIndex(compositeByteBuf.writerIndex() + boundaryByteBub.writerIndex());
                compositeByteBuf.addComponent(partByteBuf).writerIndex(compositeByteBuf.writerIndex() + partByteBuf.writerIndex());
                compositeByteBuf.addComponent(newLineByteBuf).writerIndex(compositeByteBuf.writerIndex() + newLineByteBuf.writerIndex());
                httpOutputMessage.write(compositeByteBuf);
            }
        }
    }

    private ByteBuf writeBoundary(String boundary) throws IOException {
        final ByteBuf byteBuf = Unpooled.directBuffer();
        byteBuf.writeBytes("--".getBytes());
        byteBuf.writeBytes(boundary.getBytes());
        writeNewLine(byteBuf);
        return byteBuf;
    }

    private void writeEnd(HttpOutputMessage httpOutputMessage, String boundary) throws IOException {
        final ByteBuf byteBuf = Unpooled.directBuffer();
        byteBuf.writeBytes("--".getBytes());
        byteBuf.writeBytes(boundary.getBytes());
        byteBuf.writeBytes("--".getBytes());
        writeNewLine(byteBuf);

        httpOutputMessage.write(byteBuf);
    }

    private void writeNewLine(ByteBuf byteBuf) throws IOException {
        byteBuf.writeBytes("\r\n".getBytes());
    }

    private ByteBuf writeNewLine() throws IOException {
        final ByteBuf byteBuf = Unpooled.directBuffer();
        writeNewLine(byteBuf);
        return byteBuf;
    }

    @SuppressWarnings("unchecked")
    private ByteBuf writePart(String name, Part part) throws IOException {
        final Object partBody = part.getObject();
        final Class<?> partType = partBody.getClass();
        final MultiMap partHeaders = part.getHeaders();
        final String fileName = part.getFileName();

        if (!partHeaders.contains(HttpHeaders.CONTENT_TYPE)) {
            throw new IllegalStateException("Parts headers don't contain Content-Type");
        }

        final String partContentTypeString = partHeaders.get(HttpHeaders.CONTENT_TYPE);
        final MediaType partContentType = MediaType.parseMediaType(partContentTypeString);
        for (HttpMessageConverter messageConverter : this.partConverters) {
            if (messageConverter.canWrite(partType, partContentType)) {
                final MultipartHttpOutputMessage multipartHttpOutputMessage = new MultipartHttpOutputMessage();
                setContentDispositionFormData(name, fileName, multipartHttpOutputMessage.getHeaders());
                if (!partHeaders.isEmpty()) {
                    multipartHttpOutputMessage.putAllHeaders(partHeaders);
                }
                messageConverter.write(partBody, partContentType, multipartHttpOutputMessage);
                return multipartHttpOutputMessage.getBody();
            }
        }
        throw new HttpMessageConverterException("Could not write request: no suitable HttpMessageConverter " +
                "found for request type [" + partType.getName() + "]");
    }

    private void setContentDispositionFormData(String name, String filename, MultiMap multiMap) {
        checkNotNull(name, "name must not be null");
        checkArgument(!name.isEmpty(), "name must not be empty");

        final StringBuilder builder = new StringBuilder("form-data; name=\"");
        builder.append(name).append('\"');
        if (filename != null) {
            builder.append("; filename=\"");
            builder.append(filename).append('\"');
        }
        multiMap.set(CONTENT_DISPOSITION, builder.toString());
    }


    /**
     * Generate a multipart boundary.
     * <p>The default implementation returns a random boundary.
     * Can be overridden in subclasses.
     */
    private String generateMultipartBoundary() {
        byte[] boundary = new byte[this.random.nextInt(11) + 30];
        for (int i = 0; i < boundary.length; i++) {
            boundary[i] = BOUNDARY_CHARS[this.random.nextInt(BOUNDARY_CHARS.length)];
        }

        final String constructedBoundry = "----" + new String(boundary, Charsets.US_ASCII);
        return constructedBoundry.toLowerCase();
    }
}
