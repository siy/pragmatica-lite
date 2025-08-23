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
import org.pragmatica.lang.type.TypeToken;

/// Builder interface for constructing HTTP requests in a fluent manner.
/// Provides methods to set URL, headers, body, and other request properties before execution.
public interface HttpRequestBuilder {
    
    /// Set the request URL
    HttpRequestBuilder url(String url);
    
    /// Set the HTTP method
    HttpRequestBuilder method(HttpMethod method);
    
    /// Set a header value (replaces any existing values for this header)
    HttpRequestBuilder header(String name, String value);
    
    /// Add a header value (allows multiple values for the same header)
    HttpRequestBuilder addHeader(String name, String value);
    
    /// Set all headers (replaces existing headers)
    HttpRequestBuilder headers(HttpHeaders headers);
    
    /// Set the request body
    HttpRequestBuilder body(Object body);
    
    /// Set the expected response type using Class
    <R> HttpRequestBuilder responseType(Class<R> responseType);
    
    /// Set the expected response type using TypeToken
    <R> HttpRequestBuilder responseType(TypeToken<R> responseType);
    
    // === Convenience methods for common HTTP methods ===
    
    /// Send GET request with specified response type
    <R> Promise<HttpResponse<R>> get(Class<R> responseType);
    
    /// Send GET request with TypeToken response type
    <R> Promise<HttpResponse<R>> get(TypeToken<R> responseType);
    
    /// Send GET request expecting no response body
    Promise<HttpResponse<Unit>> get();
    
    /// Send POST request with body and response type
    <R> Promise<HttpResponse<R>> post(Object body, Class<R> responseType);
    
    /// Send POST request with body and TypeToken response type
    <R> Promise<HttpResponse<R>> post(Object body, TypeToken<R> responseType);
    
    /// Send POST request with body, expecting no response body
    Promise<HttpResponse<Unit>> post(Object body);
    
    /// Send PUT request with body and response type
    <R> Promise<HttpResponse<R>> put(Object body, Class<R> responseType);
    
    /// Send PUT request with body and TypeToken response type
    <R> Promise<HttpResponse<R>> put(Object body, TypeToken<R> responseType);
    
    /// Send PUT request with body, expecting no response body
    Promise<HttpResponse<Unit>> put(Object body);
    
    /// Send PATCH request with body and response type
    <R> Promise<HttpResponse<R>> patch(Object body, Class<R> responseType);
    
    /// Send PATCH request with body and TypeToken response type
    <R> Promise<HttpResponse<R>> patch(Object body, TypeToken<R> responseType);
    
    /// Send PATCH request with body, expecting no response body
    Promise<HttpResponse<Unit>> patch(Object body);
    
    /// Send DELETE request with specified response type
    <R> Promise<HttpResponse<R>> delete(Class<R> responseType);
    
    /// Send DELETE request with TypeToken response type
    <R> Promise<HttpResponse<R>> delete(TypeToken<R> responseType);
    
    /// Send DELETE request expecting no response body
    Promise<HttpResponse<Unit>> delete();
    
    /// Build and send the request
    <T, R> Promise<HttpResponse<R>> send();
    
    /// Create a new request builder
    static HttpRequestBuilder create() {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl();
    }
    
    /// Create a new request builder with associated client
    static HttpRequestBuilder create(HttpClient client) {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl(client);
    }
}