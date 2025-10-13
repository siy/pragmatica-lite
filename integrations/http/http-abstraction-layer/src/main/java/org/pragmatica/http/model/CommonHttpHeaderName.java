/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.http.model;

/// Common HTTP header names as defined in various RFCs.
/// All names are stored in canonical lowercase form.
public enum CommonHttpHeaderName implements HttpHeaderName {
    // Request headers (RFC 7231, 7232, 7233, 7234, 7235)
    ACCEPT("accept"),
    ACCEPT_CHARSET("accept-charset"),
    ACCEPT_ENCODING("accept-encoding"),
    ACCEPT_LANGUAGE("accept-language"),
    AUTHORIZATION("authorization"),
    CACHE_CONTROL("cache-control"),
    CONNECTION("connection"),
    CONTENT_LENGTH("content-length"),
    CONTENT_TYPE("content-type"),
    COOKIE("cookie"),
    DATE("date"),
    EXPECT("expect"),
    FORWARDED("forwarded"),
    FROM("from"),
    HOST("host"),
    IF_MATCH("if-match"),
    IF_MODIFIED_SINCE("if-modified-since"),
    IF_NONE_MATCH("if-none-match"),
    IF_RANGE("if-range"),
    IF_UNMODIFIED_SINCE("if-unmodified-since"),
    MAX_FORWARDS("max-forwards"),
    ORIGIN("origin"),
    PRAGMA("pragma"),
    PROXY_AUTHORIZATION("proxy-authorization"),
    RANGE("range"),
    REFERER("referer"),
    TE("te"),
    TRAILER("trailer"),
    TRANSFER_ENCODING("transfer-encoding"),
    USER_AGENT("user-agent"),
    UPGRADE("upgrade"),
    VIA("via"),
    WARNING("warning"),

    // Response headers
    ACCEPT_RANGES("accept-ranges"),
    AGE("age"),
    ALLOW("allow"),
    CONTENT_ENCODING("content-encoding"),
    CONTENT_LANGUAGE("content-language"),
    CONTENT_LOCATION("content-location"),
    CONTENT_RANGE("content-range"),
    ETAG("etag"),
    EXPIRES("expires"),
    LAST_MODIFIED("last-modified"),
    LOCATION("location"),
    PROXY_AUTHENTICATE("proxy-authenticate"),
    RETRY_AFTER("retry-after"),
    SERVER("server"),
    SET_COOKIE("set-cookie"),
    VARY("vary"),
    WWW_AUTHENTICATE("www-authenticate"),

    // CORS headers (RFC 6454, Fetch spec)
    ACCESS_CONTROL_ALLOW_CREDENTIALS("access-control-allow-credentials"),
    ACCESS_CONTROL_ALLOW_HEADERS("access-control-allow-headers"),
    ACCESS_CONTROL_ALLOW_METHODS("access-control-allow-methods"),
    ACCESS_CONTROL_ALLOW_ORIGIN("access-control-allow-origin"),
    ACCESS_CONTROL_EXPOSE_HEADERS("access-control-expose-headers"),
    ACCESS_CONTROL_MAX_AGE("access-control-max-age"),
    ACCESS_CONTROL_REQUEST_HEADERS("access-control-request-headers"),
    ACCESS_CONTROL_REQUEST_METHOD("access-control-request-method"),

    // Security headers
    CONTENT_SECURITY_POLICY("content-security-policy"),
    STRICT_TRANSPORT_SECURITY("strict-transport-security"),
    X_CONTENT_TYPE_OPTIONS("x-content-type-options"),
    X_FRAME_OPTIONS("x-frame-options"),
    X_XSS_PROTECTION("x-xss-protection"),

    // Common custom headers
    X_FORWARDED_FOR("x-forwarded-for"),
    X_FORWARDED_HOST("x-forwarded-host"),
    X_FORWARDED_PROTO("x-forwarded-proto"),
    X_REQUEST_ID("x-request-id"),
    X_CORRELATION_ID("x-correlation-id");

    private final String headerName;

    CommonHttpHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /// Get the header name (HttpHeaderName interface method).
    /// Note: Shadows Enum.name() which returns the enum constant name.
    ///
    /// @return lowercase header name
    public String headerName() {
        return headerName;
    }

    /// Find a common header by name (case-insensitive).
    ///
    /// @param name header name to look up
    /// @return matching enum constant, or null if not found
    public static CommonHttpHeaderName fromString(String name) {
        String normalized = name.toLowerCase();
        for (CommonHttpHeaderName header : values()) {
            if (header.headerName.equals(normalized)) {
                return header;
            }
        }
        return null;
    }
}
