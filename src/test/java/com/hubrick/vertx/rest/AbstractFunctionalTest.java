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
package com.hubrick.vertx.rest;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * @author Emir Dizdarevic
 * @since 1.1.0
 */
@RunWith(VertxUnitRunner.class)
public abstract class AbstractFunctionalTest {

    protected Vertx vertx;

    private static final Pattern MULTIPART_BOUNDARY_PATTERN = Pattern.compile("multipart/form-data;boundary=(.*)");

    public static final int MOCKSERVER_PORT = 8089;
    private static MockServerClient mockServerClient = startClientAndServer(MOCKSERVER_PORT);

    protected static MockServerClient getMockServerClient() {
        return mockServerClient;
    }

    @Before
    public void before(TestContext context) {
        mockServerClient.reset();
        vertx = Vertx.vertx();
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    protected byte[] loadFile(String fileName) throws IOException {
        return toByteArray(getResource(this.getClass(), fileName));
    }

    protected String loadFileAsString(String fileName) throws IOException {
        return new String(toByteArray(getResource(this.getClass(), fileName)));
    }

    protected byte[] getMultipartBoundary(HttpRequest httpRequest) {
        final String contentType = httpRequest.getFirstHeader("Content-Type");
        final Matcher matcher = MULTIPART_BOUNDARY_PATTERN.matcher(contentType);
        matcher.matches();
        return matcher.group(1).getBytes();
    }
}
