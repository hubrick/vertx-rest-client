package com.hubrick.vertx.rest.converter;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.hubrick.vertx.rest.HttpOutputMessage;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.message.BufferedHttpOutputMessage;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author marcus
 * @since 1.0.0
 */
public class FormHttpMessageConverterUnitTest {

    @Test
    public void testConversion() {
        final FormHttpMessageConverter converter = new FormHttpMessageConverter();
        final HttpOutputMessage message = new BufferedHttpOutputMessage();

        final HashMultimap<String, Object> values = HashMultimap.create();
        values.put("test", "value");

        converter.write(values,MediaType.APPLICATION_FORM_URLENCODED, message);

        assertThat(message.getBody().toString(Charsets.UTF_8), is("test=value"));
    }

}
