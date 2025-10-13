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

import org.pragmatica.lang.Option;

import java.util.HashMap;
import java.util.Map;

/// Immutable collection of HTTP cookies.
/// Parsed from the Cookie request header.
public record Cookies(Map<String, String> cookies) {
    public Cookies {
        cookies = Map.copyOf(cookies);
    }

    /// Get a cookie value by name.
    ///
    /// @param name cookie name
    /// @return Option containing cookie value if present
    public Option<String> get(String name) {
        return Option.option(cookies.get(name));
    }

    /// Create an empty cookies collection.
    ///
    /// @return empty Cookies
    public static Cookies empty() {
        return new Cookies(Map.of());
    }

    /// Parse cookies from the Cookie header value.
    /// Cookie header format: "name1=value1; name2=value2"
    ///
    /// @param cookieHeader Cookie header value
    /// @return Cookies instance
    public static Cookies fromHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return empty();
        }

        Map<String, String> parsed = new HashMap<>();
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex > 0) {
                String name = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if (!name.isEmpty() && !value.isEmpty()) {
                    parsed.put(name, value);
                }
            }
        }
        return new Cookies(parsed);
    }

    /// Parse cookies from HttpHeaders.
    ///
    /// @param headers HTTP headers
    /// @return Cookies instance
    public static Cookies fromHeaders(HttpHeaders headers) {
        return headers.get(CommonHttpHeaderName.COOKIE)
            .map(Cookies::fromHeader)
            .or(Cookies::empty);
    }
}
