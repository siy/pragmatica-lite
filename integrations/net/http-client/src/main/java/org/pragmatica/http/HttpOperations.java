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

package org.pragmatica.http;

import org.pragmatica.lang.Promise;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;

/// Implementation-agnostic interface for HTTP operations.
/// Abstracts over HTTP client implementations to allow future extensibility.
public interface HttpOperations {

    /// Sends an HTTP request and returns the response asynchronously.
    ///
    /// @param request HTTP request to send
    /// @param handler Body handler for response processing
    /// @param <T> Response body type
    ///
    /// @return Promise of HttpResult containing status, headers, and body
    <T> Promise<HttpResult<T>> send(HttpRequest request, BodyHandler<T> handler);

    /// Sends an HTTP request expecting a String response.
    ///
    /// @param request HTTP request to send
    ///
    /// @return Promise of HttpResult with String body
    default Promise<HttpResult<String>> sendString(HttpRequest request) {
        return send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    }

    /// Sends an HTTP request expecting a byte array response.
    ///
    /// @param request HTTP request to send
    ///
    /// @return Promise of HttpResult with byte array body
    default Promise<HttpResult<byte[]>> sendBytes(HttpRequest request) {
        return send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
    }

    /// Sends an HTTP request discarding the response body.
    ///
    /// @param request HTTP request to send
    ///
    /// @return Promise of HttpResult with Void body
    default Promise<HttpResult<Void>> sendDiscarding(HttpRequest request) {
        return send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
    }
}
