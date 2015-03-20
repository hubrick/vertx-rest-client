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

import javax.net.ssl.SSLContext;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface SslSupport<T> {

    /**
     * If {@code ssl} is {@code true}, this signifies that any connections will be SSL connections.
     * @return A reference to this, so multiple invocations can be chained together.
     */
    T setSSL(boolean ssl);

    /**
     *
     * @return Is SSL enabled?
     */
    boolean isSSL();

    /**
     * If you want an SSL client to trust *all* server certificates rather than match them
     * against those in its trust store, you can set this to true.<p>
     * Use this with caution as you may be exposed to "main in the middle" attacks
     * @param trustAll Set to true if you want to trust all server certificates
     */
    T setTrustAll(boolean trustAll);

    /**
     *
     * @return true if this client will trust all server certificates.
     */
    boolean isTrustAll();

    /**
     * Set the SSL context explicitly.  This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)}
     * has been set to {@code true}.<p>
     * The SSL context has to be properly initialized.
     * Only use this method if you have very special requirements concerning your key managers, trust managers and/or
     * corresponding stores.
     */
    T setSSLContext(SSLContext sslContext);

    /**
     * Set the path to the SSL key store. This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)}
     * has been set to {@code true}.<p>
     * The SSL key store is a standard Java Key Store, and will contain the client certificate. Client certificates are
     * only required if the server requests client authentication.<p>
     * @return A reference to this, so multiple invocations can be chained together.
     */
    T setKeyStorePath(String path);

    /**
     *
     * @return Get the key store path
     */
    String getKeyStorePath();

    /**
     * Set the password for the SSL key store. This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)}
     * has been set to {@code true}.<p>
     * @return A reference to this, so multiple invocations can be chained together.
     */
    T setKeyStorePassword(String pwd);

    /**
     *
     * @return Get the key store password
     */
    String getKeyStorePassword();

    /**
     * Set the path to the SSL trust store. This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)}
     * has been set to {@code true}.<p>
     * The trust store is a standard Java Key Store, and should contain the certificates of any servers that the client trusts.
     * @return A reference to this, so multiple invocations can be chained together.
     */
    T setTrustStorePath(String path);

    /**
     *
     * @return Get the trust store path
     */
    String getTrustStorePath();

    /**
     * Set the password for the SSL trust store. This method should only be used in SSL mode, i.e. after {@link #setSSL(boolean)}
     * has been set to {@code true}.<p>
     * @return A reference to this, so multiple invocations can be chained together.
     */
    T setTrustStorePassword(String pwd);

    /**
     *
     * @return Get trust store password
     */
    String getTrustStorePassword();
}
