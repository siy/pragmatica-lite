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

package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.net.http.impl.UrlBuilder;

import java.util.List;

/// HTTP client interface with function-style API for reusable endpoint definitions.
/// Supports both immediate execution (.apply()) and reusable function creation.
/// 
/// Example usage:
/// {@code
/// // Reusable function
/// var getUser = client.from("https://api.example.com")
///                    .path("users")
///                    .pathVar(String.class)
///                    .sendJson()
///                    .get(User.class)
///                    .asJson();
/// var user = getUser.apply("123").await();
/// 
/// // One-shot call
/// var user = client.from("https://api.example.com")
///                 .path("users/123")
///                 .sendJson()
///                 .get(User.class)
///                 .asJson()
///                 .apply()
///                 .await();
/// }
public interface HttpClient {
    
    /// Function-style endpoint builder - the primary API
    /// Best for: reusable endpoint definitions, type-safe parameter handling, both one-shot and repeated calls
    default HttpFunction from(String baseUrl) {
        return fromImpl(baseUrl);
    }
    
    // === Lifecycle ===
    
    /// Start the HTTP client
    Promise<Unit> start();
    
    /// Stop the HTTP client
    Promise<Unit> stop();
    
    // === Factory Methods ===
    
    /// Create HTTP client with default configuration
    static HttpClient create() {
        return create(HttpClientConfig.defaults());
    }
    
    /// Create HTTP client with custom configuration
    static HttpClient create(HttpClientConfig config) {
        return new org.pragmatica.net.http.netty.NettyHttpClient(config);
    }
    
    // === Default Implementation ===
    
    default HttpFunction fromImpl(String baseUrl) {
        
        record httpFunction(HttpClient client, String baseUrl, 
                           UrlBuilder urlBuilder) implements HttpFunction {
            
            httpFunction {
            }
            
            @Override
            public HttpFunctionBuilder0 path(String pathSegments) {
                var segments = List.of(pathSegments.split("/"));
                return new org.pragmatica.net.http.impl.HttpFunctionBuilder0Impl(client, baseUrl, segments, urlBuilder, null);
            }
        };
        
        return new httpFunction(this, baseUrl, UrlBuilder.create());
    }
}