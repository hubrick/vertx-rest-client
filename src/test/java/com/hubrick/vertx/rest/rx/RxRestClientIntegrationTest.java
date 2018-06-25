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
package com.hubrick.vertx.rest.rx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.hubrick.vertx.rest.AbstractFunctionalTest;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RequestCacheOptions;
import com.hubrick.vertx.rest.RestClientOptions;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.FormHttpMessageConverter;
import com.hubrick.vertx.rest.converter.JacksonJsonHttpMessageConverter;
import com.hubrick.vertx.rest.converter.MultipartHttpMessageConverter;
import com.hubrick.vertx.rest.converter.StringHttpMessageConverter;
import com.hubrick.vertx.rest.converter.model.Part;
import io.netty.buffer.Unpooled;
import io.vertx.core.MultiMap;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.apache.commons.fileupload.MultipartStream;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import rx.Observable;
import rx.Single;
import rx.functions.Func0;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.hubrick.vertx.rest.VertxMatcherAssert.assertThat;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Emir Dizdarevic
 * @since 1.1.0
 */
public class RxRestClientIntegrationTest extends AbstractFunctionalTest {

    private RxRestClient rxRestClient;

    @Before
    public void setUp() {
        final RestClientOptions clientOptions = new RestClientOptions();
        clientOptions.setDefaultHost("localhost");
        clientOptions.setDefaultPort(MOCKSERVER_PORT);
        clientOptions.setMaxPoolSize(10);
        clientOptions.setGlobalRequestTimeout(1000000);

        createAndSetClient(clientOptions);
    }

    private void createAndSetClient(final RestClientOptions clientOptions) {
        rxRestClient = RxRestClient.create(
                vertx,
                clientOptions,
                ImmutableList.of(
                        new FormHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new JacksonJsonHttpMessageConverter(new ObjectMapper())
                )
        );
    }

    @Test
    public void testMultipartByteBuffer(TestContext testContext) throws Exception {
        testMultipart(testContext, new Part(ByteBuffer.wrap(loadFile("test.gif")), "test.gif").setContentType(MediaType.IMAGE_GIF));
    }

    @Test
    public void testMultipartByteBuf(TestContext testContext) throws Exception {
        testMultipart(testContext, new Part(Unpooled.wrappedBuffer(loadFile("test.gif")), "test.gif").setContentType(MediaType.IMAGE_GIF));
    }

    @Test
    public void testMultipartByteArray(TestContext testContext) throws Exception {
        testMultipart(testContext, new Part(loadFile("test.gif"), "test.gif").setContentType(MediaType.IMAGE_GIF));
    }

    private void testMultipart(TestContext testContext, Part imagePart) throws Exception {
        final HttpRequest multipartHttpRequest = request()
                .withMethod("POST")
                .withPath("/api/v1/images")
                .withHeader(Header.header("Content-Type", "multipart/form-data.*"));
        getMockServerClient().when(
                multipartHttpRequest
        ).respond(
                response()
                        .withStatusCode(200)
        );

        final RestClientOptions clientOptions = new RestClientOptions();
        clientOptions.setDefaultHost("localhost");
        clientOptions.setDefaultPort(MOCKSERVER_PORT);
        clientOptions.setMaxPoolSize(10);

        final MultipartHttpMessageConverter multipartHttpMessageConverter = new MultipartHttpMessageConverter();
        multipartHttpMessageConverter.addPartConverter(new JacksonJsonHttpMessageConverter(new ObjectMapper()));

        final RxRestClient rxRestClient = RxRestClient.create(
                vertx,
                clientOptions,
                ImmutableList.of(multipartHttpMessageConverter)
        );

        final HashMultimap<String, Part> parts = HashMultimap.create();
        parts.put("image", imagePart);
        parts.put("request", new Part(new ConvertRequest("caption"), MultiMap.caseInsensitiveMultiMap().add("Content-Type", "application/json")));

        final Async async = testContext.async();
        rxRestClient.post("/api/v1/images", restClientRequest -> {
            restClientRequest.setContentType(MediaType.MULTIPART_FORM_DATA);
            restClientRequest.end(parts);
        }).subscribe(
                restClientResponse -> {
                    try {
                        assertThat(testContext, restClientResponse.statusCode(), is(200));

                        final HttpRequest[] requests = getMockServerClient().retrieveRecordedRequests(multipartHttpRequest);
                        assertThat(testContext, Arrays.asList(requests), hasSize(1));

                        final ByteArrayInputStream content = new ByteArrayInputStream(requests[0].getBodyAsRawBytes());
                        final MultipartStream multipartStream = new MultipartStream(content, getMultipartBoundary(requests[0]));

                        final ByteArrayOutputStream image = new ByteArrayOutputStream();
                        final ByteArrayOutputStream request = new ByteArrayOutputStream();

                        final String imageHeaders = multipartStream.readHeaders();
                        multipartStream.readBodyData(image);
                        multipartStream.readBoundary();

                        final String requestHeaders = multipartStream.readHeaders();
                        multipartStream.readBodyData(request);
                        multipartStream.readBoundary();

                        assertThat(testContext, requestHeaders, containsString("Content-Type: application/json"));
                        assertThat(testContext, imageHeaders, containsString("filename="));
                        assertThat(testContext, image.toByteArray(), is(loadFile("test.gif")));
                        assertThat(testContext, new String(request.toByteArray(), Charsets.UTF_8), jsonEquals("{\"caption\": \"caption\"}"));
                    } catch (Exception e) {
                        testContext.fail(e);
                    }
                    async.complete();
                },
                testContext::fail
        );
    }

    @Test
    public void testSimpleRxFlow(TestContext testContext) throws Exception {
        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/search")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientIntegrationTest.class, "userSearchResponse.json")))
        );

        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientIntegrationTest.class, "userResponse1.json")))
        );

        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/b9d8fb1a-38c5-45ea-a7ee-6450a964f4f8")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientIntegrationTest.class, "userResponse2.json")))
        );

        final RestClientOptions clientOptions = new RestClientOptions();
        clientOptions.setDefaultHost("localhost");
        clientOptions.setDefaultPort(MOCKSERVER_PORT);
        clientOptions.setMaxPoolSize(10);

        final RxRestClient rxRestClient = RxRestClient.create(
                vertx,
                clientOptions,
                ImmutableList.of(
                        new FormHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new JacksonJsonHttpMessageConverter(new ObjectMapper())
                )
        );

        final Async async = testContext.async();
        rxRestClient.get("/api/v1/users/search", UserSearchResponse[].class, RestClientRequest::end)
                .flatMapObservable(userSearchResponseRestClientResponse -> {
                    final List<Observable<RestClientResponse<UserResponse>>> responses = new LinkedList<>();
                    for (UserSearchResponse userSearchResponse : userSearchResponseRestClientResponse.getBody()) {
                        final Single<RestClientResponse<UserResponse>> userResponse = rxRestClient.get("/api/v1/users/" + userSearchResponse.getId(), UserResponse.class, RestClientRequest::end);
                        responses.add(userResponse.toObservable());
                    }

                    return Observable.merge(responses);
                }).subscribe(
                userResponseRestClientResponse -> {
                    final Set<UUID> ids = Sets.newHashSet(UUID.fromString("b9d8fb1a-38c5-45ea-a7ee-6450a964f4f8"), UUID.fromString("e5297618-c299-4157-a85c-4957c8204819"));
                    final UUID id = userResponseRestClientResponse.getBody().getId();
                    assertThat(testContext, id, isIn(ids));
                },
                testContext::fail,
                async::complete
        );
    }

    @Test
    public void testRequestWithCache(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(10000), 1);
    }

    @Test
    public void testRequestWithCache_NotFoundHttpResponse(TestContext testContext) {
        testRequestCacheNotFound(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(10000), 1);
    }

    @Test
    public void testRequestWithCache_UnknownHostError(TestContext testContext) {
        final RestClientOptions clientOptions = new RestClientOptions();
        clientOptions.setDefaultHost("thisIsAnUnknownHost");
        createAndSetClient(clientOptions);

        final Async async = testContext.async(3);

        final Func0<Single<UserResponse>> request = () -> rxRestClient.get(
                "/api/v1/users/e5297618-c299-4157-a85c-4957c8204819",
                UserResponse.class,
                restClientRequest -> restClientRequest.setRequestCache(new RequestCacheOptions().withExpiresAfterWriteMillis(10000)).end()
        ).map(RestClientResponse::getBody);

        Observable.concatEager(
                Single.defer(request).map(userResponse -> new Throwable("not expected"))
                        .onErrorResumeNext(Single::just).toObservable(),
                Single.defer(request).map(userResponse -> new Throwable("not expected"))
                        .onErrorResumeNext(Single::just).toObservable(),
                Single.defer(request).map(userResponse -> new Throwable("not expected"))
                        .onErrorResumeNext(Single::just).toObservable()
        ).subscribe(
                throwable -> {
                    if (throwable instanceof UnknownHostException) {
                        async.countDown();
                    } else {
                        testContext.fail("Received unexpected exception");
                    }
                },
                testContext::fail
        );
    }

    @Test
    public void testRequestWithoutCache(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, null, 3);
    }

    @Test
    public void testRequestWithCacheEvictAll(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(10000).withEvictAllBefore(true), 3);
    }

    @Test
    public void testRequestWithCacheEvicted(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 1);

        // Wait until evicted
        Thread.sleep(5000);

        getMockServerClient().reset();
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 1);
    }

    @Test
    public void testRequestWithCacheNotEvicted(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 1);

        Thread.sleep(1000);

        getMockServerClient().reset();
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 0);
    }

    @Test
    public void testRequestWithCacheEvictedWithExpiresAfterOnAccess(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000).withExpiresAfterAccessMillis(500), 1);

        // Wait until evicted
        Thread.sleep(1000);

        getMockServerClient().reset();
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 0);
    }

    @Test
    public void testRequestWithCacheNotEvictedWithExpiresAfterOnAccess(TestContext testContext) throws Exception {
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000).withExpiresAfterAccessMillis(2000), 1);

        Thread.sleep(1000);

        getMockServerClient().reset();
        testRequestCacheOk(testContext, new RequestCacheOptions().withExpiresAfterWriteMillis(4000), 0);
    }

    private void testRequestCacheOk(TestContext testContext, RequestCacheOptions requestCacheOptions, int timesCalled) throws Exception {

        final HttpRequest httpRequest = request().withMethod("GET").withPath("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819");
        getMockServerClient().when(
                httpRequest
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientIntegrationTest.class, "userResponse1.json")))
        );

        final Async async = testContext.async();
        final Func0<Observable<UserResponse>> request = () -> rxRestClient.get("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819", UserResponse.class, restClientRequest -> restClientRequest.setRequestCache(requestCacheOptions).end())
                .map(RestClientResponse::getBody)
                .toObservable();

        Observable.concatEager(Observable.defer(request), Observable.defer(request), Observable.defer(request))
                .subscribe(
                        aVoid -> {
                            // Do nothing
                        },
                        testContext::fail,
                        () -> {
                            assertThat(testContext, Arrays.asList(getMockServerClient().retrieveRecordedRequests(httpRequest)), hasSize(timesCalled));
                            async.complete();
                        }
                );
    }

    private void testRequestCacheNotFound(TestContext testContext, RequestCacheOptions requestCacheOptions, int timesCalled) {

        final HttpRequest httpRequest = request().withMethod("GET").withPath("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819");
        getMockServerClient().when(
                httpRequest
        ).respond(
                response().withStatusCode(404)
        );

        final Async async = testContext.async();
        final Func0<Observable<UserResponse>> request = () -> rxRestClient.get("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819", UserResponse.class, restClientRequest -> restClientRequest.setRequestCache(requestCacheOptions).end())
                .map(RestClientResponse::getBody)
                .toObservable();

        Observable.concatEager(
                Observable.defer(request).onErrorResumeNext(throwable -> Observable.just(null)),
                Observable.defer(request).onErrorResumeNext(throwable -> Observable.just(null)),
                Observable.defer(request).onErrorResumeNext(throwable -> Observable.just(null))
        ).subscribe(
                aVoid -> {
                    // Do nothing
                },
                testContext::fail,
                () -> {
                    assertThat(testContext, Arrays.asList(getMockServerClient().retrieveRecordedRequests(httpRequest)), hasSize(timesCalled));
                    async.complete();
                }
        );
    }

}
