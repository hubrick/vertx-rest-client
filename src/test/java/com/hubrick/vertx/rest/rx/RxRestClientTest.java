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
import com.google.common.collect.ImmutableList;
import com.hubrick.vertx.rest.AbstractFunctionalTest;
import com.hubrick.vertx.rest.RestClient;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.FormHttpMessageConverter;
import com.hubrick.vertx.rest.converter.JacksonJsonHttpMessageConverter;
import com.hubrick.vertx.rest.converter.StringHttpMessageConverter;
import com.hubrick.vertx.rest.impl.DefaultRestClient;
import com.hubrick.vertx.rest.rx.impl.DefaultRxRestClient;
import org.junit.Test;
import org.mockserver.model.Header;
import org.vertx.testtools.VertxAssert;
import rx.Observable;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Emir Dizdarevic
 * @since 1.1.0
 */
public class RxRestClientTest extends AbstractFunctionalTest {

    @Test
    public void testSimpleGETRequest() throws Exception {
        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/search")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientTest.class, "userSearchResponse.json")))
        );

        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/e5297618-c299-4157-a85c-4957c8204819")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientTest.class, "userResponse1.json")))
        );

        getMockServerClient().when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/b9d8fb1a-38c5-45ea-a7ee-6450a964f4f8")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader(Header.header("Content-Type", "application/json;charset=UTF-8"))
                        .withBody(toByteArray(getResource(RxRestClientTest.class, "userResponse2.json")))
        );


        final RestClient restClient = new DefaultRestClient(
                vertx,
                ImmutableList.of(
                        new FormHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new JacksonJsonHttpMessageConverter(new ObjectMapper())
                )
        );

        restClient
                .setHost("localhost")
                .setPort(8089)
                .setMaxPoolSize(10);

        final RxRestClient rxRestClient = new DefaultRxRestClient(restClient);
        final Observable<RestClientResponse<UserSearchResponse[]>> response =  rxRestClient.get("/api/v1/users/search", UserSearchResponse[].class, restClientRequest -> restClientRequest.end());
        response.flatMap(userSearchResponseRestClientResponse -> {
            final List<Observable<RestClientResponse<UserResponse>>> responses = new LinkedList<>();
            for (UserSearchResponse userSearchResponse : userSearchResponseRestClientResponse.getBody()) {
                final Observable<RestClientResponse<UserResponse>> userResponse = rxRestClient.get("/api/v1/users/" + userSearchResponse.getId(), UserResponse.class, restClientRequest -> restClientRequest.end());
                responses.add(userResponse);
            }

            return Observable.merge(responses);
        }).finallyDo(() -> VertxAssert.testComplete())
                .forEach(userResponseRestClientResponse -> assertThat(userResponseRestClientResponse.getBody().getId(), anyOf(is("b9d8fb1a-38c5-45ea-a7ee-6450a964f4f8"), is("e5297618-c299-4157-a85c-4957c8204819"))));

    }
}
