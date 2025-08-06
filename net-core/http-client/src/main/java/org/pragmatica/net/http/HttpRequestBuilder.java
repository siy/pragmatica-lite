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

/// Template-style HTTP request builder for complex URL construction and flexible request building.
/// Best for: complex URLs, migration from existing string-based APIs, dynamic URL construction.
///
/// Example usage:
/// {@code
/// var response = client.request()
///     .url("https://api.example.com/users/{}/posts/{}", userId, postId)
///     .header("Authorization", "Bearer " + token)
///     .get(User.class)
///     .await();
/// }
public interface HttpRequestBuilder {
    
    // === URL Configuration ===
    
    /// Set URL with template support and positional arguments
    /// Example: url("https://api.example.com/users/{}/posts/{}", userId, postId)
    HttpRequestBuilder url(String urlTemplate, Object... params);
    
    /// Set URL directly without template processing
    HttpRequestBuilder url(String url);
    
    // === Request Configuration ===
    
    /// Set HTTP method
    HttpRequestBuilder method(HttpMethod method);
    
    /// Add a header
    HttpRequestBuilder header(String name, String value);
    
    /// Set all headers (replaces existing headers)
    HttpRequestBuilder headers(HttpHeaders headers);
    
    /// Set request body
    HttpRequestBuilder body(Object body);
    
    /// Add query parameter
    HttpRequestBuilder queryParam(String name, String value);
    
    // === Response Type Configuration ===
    
    /// Configure expected response type
    <T> HttpRequestBuilder responseType(Class<T> type);
    
    /// Configure expected response type with generic support
    <T> HttpRequestBuilder responseType(TypeToken<T> type);
    
    // === Terminal Operations ===
    
    /// Execute request with configured method and response type
    <T> Promise<Result<HttpResponse<T>>> send();
    
    /// Execute GET request
    <T> Promise<Result<HttpResponse<T>>> get(Class<T> responseType);
    
    /// Execute GET request with generic response type
    <T> Promise<Result<HttpResponse<T>>> get(TypeToken<T> responseType);
    
    /// Execute GET request, no response body expected
    Promise<Result<HttpResponse<Unit>>> get();
    
    /// Execute POST request
    <T> Promise<Result<HttpResponse<T>>> post(Object body, Class<T> responseType);
    
    /// Execute POST request with generic response type
    <T> Promise<Result<HttpResponse<T>>> post(Object body, TypeToken<T> responseType);
    
    /// Execute POST request, no response body expected
    Promise<Result<HttpResponse<Unit>>> post(Object body);
    
    /// Execute PUT request
    <T> Promise<Result<HttpResponse<T>>> put(Object body, Class<T> responseType);
    
    /// Execute PUT request with generic response type
    <T> Promise<Result<HttpResponse<T>>> put(Object body, TypeToken<T> responseType);
    
    /// Execute PUT request, no response body expected
    Promise<Result<HttpResponse<Unit>>> put(Object body);
    
    /// Execute PATCH request
    <T> Promise<Result<HttpResponse<T>>> patch(Object body, Class<T> responseType);
    
    /// Execute PATCH request with generic response type
    <T> Promise<Result<HttpResponse<T>>> patch(Object body, TypeToken<T> responseType);
    
    /// Execute PATCH request, no response body expected
    Promise<Result<HttpResponse<Unit>>> patch(Object body);
    
    /// Execute DELETE request
    <T> Promise<Result<HttpResponse<T>>> delete(Class<T> responseType);
    
    /// Execute DELETE request with generic response type
    <T> Promise<Result<HttpResponse<T>>> delete(TypeToken<T> responseType);
    
    /// Execute DELETE request, no response body expected
    Promise<Result<HttpResponse<Unit>>> delete();
}