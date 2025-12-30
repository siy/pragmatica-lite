/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.lang.parse;

import org.pragmatica.lang.Result;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

/// Functional wrappers for JDK network and identifier parsing APIs that return Result<T> instead of throwing exceptions
public sealed interface Network {
    /// Parse a string as a URL value
    ///
    /// @param spec String to parse as URL
    ///
    /// @return Result containing parsed URL or parsing error
    static Result<URL> parseURL(String spec) {
        return parseURI(spec).flatMap(uri -> Result.lift(uri::toURL));
    }

    /// Parse a string as a URI value
    ///
    /// @param str String to parse as URI
    ///
    /// @return Result containing parsed URI or parsing error
    static Result<URI> parseURI(String str) {
        return Result.lift1(URI::new, str);
    }

    /// Parse a string as a UUID value
    ///
    /// @param name String to parse as UUID
    ///
    /// @return Result containing parsed UUID or parsing error
    static Result<UUID> parseUUID(String name) {
        return Result.lift1(UUID::fromString, name);
    }

    /// Parse a string as an InetAddress value
    ///
    /// @param host String to parse as InetAddress (hostname or IP address)
    ///
    /// @return Result containing parsed InetAddress or parsing error
    static Result<InetAddress> parseInetAddress(String host) {
        return Result.lift1(InetAddress::getByName, host);
    }

    record unused() implements Network {}
}
