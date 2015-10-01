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
package com.hubrick.vertx.rest.exception;

import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import io.vertx.core.http.HttpClientResponse;

import java.util.List;

/**
 * Exception thrown when an HTTP 5xx is received.
 *
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public class HttpServerErrorException extends HttpStatusCodeException {

    private static final long serialVersionUID = -2915754006618138282L;

    public HttpServerErrorException(HttpClientResponse httpClientResponse, List<HttpMessageConverter> httpMessageConverters, byte[] responseBody) {
        super(httpClientResponse, httpMessageConverters, responseBody);
    }
}
