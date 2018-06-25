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
package com.hubrick.vertx.rest.rx.impl;

import com.hubrick.vertx.rest.RestClient;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.rx.RxRestClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.functions.Action1;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ColdRxRestClientTest {

    @Mock
    private RestClient client;

    @Mock
    private RestClientRequest request;

    @Before
    public void setupSafeClient() {
        when(client.request(any(), any(), any(), any())).thenReturn(request);
    }

    @Test
    public void shouldNotExecuteHandlerWithoutSubscription() {
        //given
        RxRestClient rxRestClient = new ColdRxRestClient(client);

        //when
        rxRestClient.get("any", request -> fail("Shouldn't be called"));

        //then
        //Should not execute action with no subscribers on observable
    }

    @Test
    public void shouldExecuteHandlerOnSubscription() {
        //given
        RxRestClient rxRestClient = new ColdRxRestClient(client);
        Action1<RestClientRequest<Void>> handler = createSpyableAction();

        //when
        rxRestClient.get("any", handler).subscribe();

        //then
        Mockito.verify(handler).call(any());
    }

    @Test
    public void shouldExecuteHandlerOnEachSubscription() {
        //given
        RxRestClient rxRestClient = new ColdRxRestClient(client);
        Action1<RestClientRequest<Void>> handler = createSpyableAction();

        //when
        Observable<RestClientResponse<Void>> result = rxRestClient.get("any", handler);
        result.subscribe();
        result.subscribe();

        //then
        Mockito.verify(handler, times(2)).call(any());
    }

    @Test
    public void shouldExecuteHandlerImmediatelyWhenHot() {
        //given
        RxRestClient rxRestClient = new ColdRxRestClient(client);
        Action1<RestClientRequest<Void>> handler = createSpyableAction();

        //when
        rxRestClient.get("any", handler).publish().connect();

        //then
        Mockito.verify(handler).call(any());
    }

    @SuppressWarnings("unchecked")
    private Action1<RestClientRequest<Void>> createSpyableAction() {
        return (Action1<RestClientRequest<Void>>) Mockito.spy(Action1.class);
    }
}