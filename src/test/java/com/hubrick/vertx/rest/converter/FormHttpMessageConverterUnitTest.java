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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.message.BufferedHttpInputMessage;
import com.hubrick.vertx.rest.message.BufferedHttpOutputMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author marcus
 * @since 2.2.2
 */
public class FormHttpMessageConverterUnitTest {

    @Test
    public void testWrite() {
        final FormHttpMessageConverter converter = new FormHttpMessageConverter();
        final HttpOutputMessage message = new BufferedHttpOutputMessage();

        final HashMultimap<String, Object> values = HashMultimap.create();
        values.put("test", "value");

        converter.write(values, MediaType.APPLICATION_FORM_URLENCODED, message);

        assertThat(message.getBody().toString(Charsets.UTF_8), is("test=value"));
    }

    @Test
    public void testRead() {
        final FormHttpMessageConverter converter = new FormHttpMessageConverter();

        final ByteBuf byteBuf = Unpooled.wrappedBuffer("test=value".getBytes(Charsets.UTF_8));
        final Class<? extends Multimap<String, Object>> clazz = (Class<? extends Multimap<String, Object>>) HashMultimap.<String, Object>create().getClass();
        final Multimap<String, Object> values = converter.read(clazz, new BufferedHttpInputMessage(byteBuf, MultiMap.caseInsensitiveMultiMap().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE), MultiMap.caseInsensitiveMultiMap(), "Ok", 200, Collections.emptyList()));

        assertThat(values.size(), is(1));
        assertThat(values.get("test"), is(ImmutableSet.of("value")));
    }
}
