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
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;

/// Resource-style HTTP DSL for immediate execution.
/// Best for: one-off requests, exploratory API calls, simple CRUD operations.
/// 
/// Example usage:
/// {@code
/// var user = client.resource("https://api.example.com")
///     .path("api/v1/users").pathVar("userId", "123")
///     .get(User.class)
///     .await();
/// }
public interface HttpResource {
    
    // === Path Building ===
    
    /// Add path segments. Supports multiple segments separated by "/"
    /// Example: path("api/v1/users") or path("users")
    HttpResource path(String pathSegments);
    
    /// Add a path variable. The value will be URL-encoded automatically.
    /// Example: pathVar("userId", "123") adds "123" as the next path segment
    HttpResource pathVar(String name, String value);
    
    /// Add path using a template with positional arguments
    /// Example: pathTemplate("/users/{}/posts/{}", userId, postId)
    HttpResource pathTemplate(String template, Object... args);
    
    // === Request Configuration ===
    
    /// Add a query parameter
    HttpResource queryParam(String name, String value);
    
    /// Add a header
    HttpResource header(String name, String value);
    
    /// Replace all headers
    HttpResource headers(HttpHeaders headers);
    
    // === Terminal Operations (Execute Request) ===
    
    /// Execute GET request with typed response
    <T> Promise<Result<HttpResponse<T>>> get(Class<T> responseType);
    
    /// Execute GET request with generic response type
    <T> Promise<Result<HttpResponse<T>>> get(TypeToken<T> responseType);
    
    /// Execute GET request, no response body expected
    Promise<Result<HttpResponse<Unit>>> get();
    
    /// Execute POST request with body and typed response
    <T> Promise<Result<HttpResponse<T>>> post(Object body, Class<T> responseType);
    
    /// Execute POST request with body and generic response type
    <T> Promise<Result<HttpResponse<T>>> post(Object body, TypeToken<T> responseType);
    
    /// Execute POST request with body, no response body expected
    Promise<Result<HttpResponse<Unit>>> post(Object body);
    
    /// Execute PUT request with body and typed response
    <T> Promise<Result<HttpResponse<T>>> put(Object body, Class<T> responseType);
    
    /// Execute PUT request with body and generic response type
    <T> Promise<Result<HttpResponse<T>>> put(Object body, TypeToken<T> responseType);
    
    /// Execute PUT request with body, no response body expected
    Promise<Result<HttpResponse<Unit>>> put(Object body);
    
    /// Execute PATCH request with body and typed response
    <T> Promise<Result<HttpResponse<T>>> patch(Object body, Class<T> responseType);
    
    /// Execute PATCH request with body and generic response type
    <T> Promise<Result<HttpResponse<T>>> patch(Object body, TypeToken<T> responseType);
    
    /// Execute PATCH request with body, no response body expected
    Promise<Result<HttpResponse<Unit>>> patch(Object body);
    
    /// Execute DELETE request with typed response
    <T> Promise<Result<HttpResponse<T>>> delete(Class<T> responseType);
    
    /// Execute DELETE request with generic response type
    <T> Promise<Result<HttpResponse<T>>> delete(TypeToken<T> responseType);
    
    /// Execute DELETE request, no response body expected
    Promise<Result<HttpResponse<Unit>>> delete();
    
    // === Advanced Access ===
    
    /// Get access to the underlying request builder for more complex operations
    HttpRequestBuilder request();
}