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
package com.hubrick.vertx.rest.converter.model;

import org.vertx.java.core.MultiMap;

/**
 * @author Emir Dizdarevic
 * @since 1.3.0
 */
public class Part<T> {

    private final T object;
    private final MultiMap headers;
    private final String fileName;

    public Part(T object, MultiMap headers) {
        this(object, headers, null);
    }

    public Part(T object, MultiMap headers, String fileName) {
        this.object = object;
        this.headers = headers;
        this.fileName = fileName;
    }

    public T getObject() {
        return object;
    }

    public MultiMap getHeaders() {
        return headers;
    }

    public String getFileName() {
        return fileName;
    }
}
