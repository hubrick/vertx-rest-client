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

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Emir Dizdarevic
 * @since 2.0.7
 */
public class RequestCacheOptions {

    private static final int DEFAULT_REQUEST_CACHE_TTL_IN_MILLIS = 3000;
    private static final boolean DEFAULT_EVICT_BEFORE = false;
    private static final boolean DEFAULT_EVICT_ALL_BEFORE = false;
    private static final Set<Integer> DEFAULT_CACHED_STATUS_CODES = Collections.singleton(200);

    private int ttlInMillis = DEFAULT_REQUEST_CACHE_TTL_IN_MILLIS;
    private boolean evictBefore = DEFAULT_EVICT_BEFORE;
    private boolean evictAllBefore = DEFAULT_EVICT_ALL_BEFORE;
    private Set<Integer> cachedStatusCodes = DEFAULT_CACHED_STATUS_CODES;

    /**
     * Sets the time to live for the request cache entries. Default is 3000 millis.
     * It will override the global settings.
     *
     * @param ttlInMillis The quantity of time in milliseconds.
     * @return A reference to this, so multiple method calls can be chained.
     */
    public RequestCacheOptions withTtlInMillis(int ttlInMillis) {
        checkArgument(ttlInMillis > 0, "ttlInMillis must be greater then 0");
        this.ttlInMillis = ttlInMillis;
        return this;
    }

    public int getTtlInMillis() {
        return ttlInMillis;
    }

    /**
     * If an eviction for the same request should happen before fetching
     *
     * @param evictBefore If set to true this request will be evicted and re-cached
     * @return A reference to this, so multiple method calls can be chained.
     */
    public RequestCacheOptions withEvictBefore(boolean evictBefore) {
        this.evictBefore = evictBefore;
        return this;
    }

    public boolean getEvictBefore() {
        return evictBefore;
    }

    /**
     * If an eviction for the whole cache should happen before fetching.
     *
     * @param evictAllBefore If set to true the whole cache will be evicted
     * @return A reference to this, so multiple method calls can be chained.
     */
    public RequestCacheOptions withEvictAllBefore(boolean evictAllBefore) {
        this.evictAllBefore = evictAllBefore;
        return this;
    }

    public boolean getEvictAllBefore() {
        return evictAllBefore;
    }

    /**
     * Defines the status codes which will be cached. Default is 200 only.
     *
     * @param cachedStatusCodes The status codes to cache
     * @return A reference to this, so multiple method calls can be chained.
     */
    public RequestCacheOptions withCachedStatusCodes(Set<Integer> cachedStatusCodes) {
        checkNotNull(cachedStatusCodes, "cachedStatusCodes must not be null");
        this.cachedStatusCodes = cachedStatusCodes;
        return this;
    }

    public Set<Integer> getCachedStatusCodes() {
        return Collections.unmodifiableSet(cachedStatusCodes);
    }


}
