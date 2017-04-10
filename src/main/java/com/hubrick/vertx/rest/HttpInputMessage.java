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

import io.vertx.core.MultiMap;

import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public interface HttpInputMessage extends HttpMessage {

    /**
     * Return the trailers of this message.
     * @return a corresponding HttpHeaders object
     */
    MultiMap getTrailers();

    /**
     * Return the http status message
     * @return http status message
     */
    String getStatusMessage();

    /**
     * Return the http status code
     * @return http status message
     */
    Integer getStatusCode();

    /**
     * Return the cookies
     * @return cookies
     */
    List<String> getCookies();
}
