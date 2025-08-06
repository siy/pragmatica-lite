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

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.impl.UrlBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/// Main HTTP client interface providing three complementary API styles:
/// 1. Resource-style DSL for immediate execution
/// 2. Function-style DSL for reusable endpoint definitions  
/// 3. Template-style for string interpolation
public interface HttpClient {
    
    // === Three Main API Styles ===
    
    /// Resource-style DSL for immediate execution
    /// Best for: one-off requests, exploratory API calls, simple CRUD operations
    HttpResource resource(String baseUrl);
    
    /// Function-style DSL for reusable endpoint definitions
    /// Best for: API clients, repeated calls with different parameters, type-safe parameter handling
    HttpFunction function(String baseUrl);
    
    /// Template-style request builder
    /// Best for: complex URLs, migration from existing string-based APIs, dynamic URL construction
    HttpRequestBuilder request();
    
    // === Direct Template Methods ===
    
    /// Direct GET with URL template
    <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, Class<R> responseType);
    
    /// Direct GET with URL template and TypeToken
    <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, TypeToken<R> responseType);
    
    /// Direct GET with URL template, no response body expected
    Promise<HttpResponse<Unit>> get(String urlTemplate, Object[] params);
    
    /// Direct POST with URL template
    <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, Class<R> responseType);
    
    /// Direct POST with URL template and TypeToken
    <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, TypeToken<R> responseType);
    
    /// Direct POST with URL template, no response body expected
    Promise<HttpResponse<Unit>> post(String urlTemplate, Object[] params, Object body);
    
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
    
    // === Default Implementations ===
    
    default HttpResource resourceImpl(String baseUrl) {
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        
        record httpResource(HttpClient client, String baseUrl, 
                           List<String> pathSegments, Map<String, String> queryParams,
                           HttpHeaders headers, UrlBuilder urlBuilder) implements HttpResource {
            
            httpResource {
                Objects.requireNonNull(client, "Client cannot be null");
                Objects.requireNonNull(baseUrl, "Base URL cannot be null");
                Objects.requireNonNull(pathSegments, "Path segments cannot be null");
                Objects.requireNonNull(queryParams, "Query params cannot be null");
                Objects.requireNonNull(headers, "Headers cannot be null");
                Objects.requireNonNull(urlBuilder, "URL builder cannot be null");
            }
            
            @Override
            public HttpResource path(String pathSegments) {
                Objects.requireNonNull(pathSegments, "Path segments cannot be null");
                var segments = pathSegments.split("/");
                var newSegments = new java.util.ArrayList<>(this.pathSegments);
                for (var segment : segments) {
                    if (!segment.isEmpty()) {
                        newSegments.add(segment);
                    }
                }
                return new httpResource(client, baseUrl, newSegments, queryParams, headers, urlBuilder);
            }
            
            @Override
            public HttpResource pathVar(String name, String value) {
                Objects.requireNonNull(name, "Path variable name cannot be null");
                Objects.requireNonNull(value, "Path variable value cannot be null");
                return path(value);
            }
            
            @Override
            public HttpResource pathTemplate(String template, Object... args) {
                Objects.requireNonNull(template, "Template cannot be null");
                Objects.requireNonNull(args, "Arguments cannot be null");
                var resolvedPath = urlBuilder.resolveTemplate(template, args);
                return path(resolvedPath);
            }
            
            @Override
            public HttpResource queryParam(String name, String value) {
                Objects.requireNonNull(name, "Query param name cannot be null");
                Objects.requireNonNull(value, "Query param value cannot be null");
                var newParams = new java.util.HashMap<>(queryParams);
                newParams.put(name, value);
                return new httpResource(client, baseUrl, pathSegments, newParams, headers, urlBuilder);
            }
            
            @Override
            public HttpResource header(String name, String value) {
                Objects.requireNonNull(name, "Header name cannot be null");
                Objects.requireNonNull(value, "Header value cannot be null");
                var newHeaders = new HttpHeaders();
                // Copy existing headers
                for (var headerName : headers.names()) {
                    for (var headerValue : headers.all(headerName)) {
                        newHeaders.add(headerName, headerValue);
                    }
                }
                newHeaders.add(name, value);
                return new httpResource(client, baseUrl, pathSegments, queryParams, newHeaders, urlBuilder);
            }
            
            @Override
            public HttpResource headers(HttpHeaders headers) {
                Objects.requireNonNull(headers, "Headers cannot be null");
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder);
            }
            
            @Override
            public <T> Promise<Result<HttpResponse<T>>> get(Class<T> responseType) {
                Objects.requireNonNull(responseType, "Response type cannot be null");
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).get(responseType);
            }
            
            @Override
            public <T> Promise<Result<HttpResponse<T>>> get(TypeToken<T> responseType) {
                Objects.requireNonNull(responseType, "Response type cannot be null");
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).get(responseType);
            }
            
            @Override
            public Promise<Result<HttpResponse<Unit>>> get() {
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).get();
            }
            
            @Override
            public <T> Promise<Result<HttpResponse<T>>> post(Object body, Class<T> responseType) {
                Objects.requireNonNull(responseType, "Response type cannot be null");
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).body(body).post(responseType);
            }
            
            @Override
            public <T> Promise<Result<HttpResponse<T>>> post(Object body, TypeToken<T> responseType) {
                Objects.requireNonNull(responseType, "Response type cannot be null");
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).body(body).post(responseType);
            }
            
            @Override
            public Promise<Result<HttpResponse<Unit>>> post(Object body) {
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers).body(body).post();
            }
            
            @Override
            public HttpRequestBuilder request() {
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers);
            }
        };
        
        return new httpResource(this, baseUrl, emptyList(), emptyMap(), new HttpHeaders(), UrlBuilder.create());
    }
    
    default HttpFunction functionImpl(String baseUrl) {
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        
        record httpFunction(HttpClient client, String baseUrl, 
                           UrlBuilder urlBuilder) implements HttpFunction {
            
            httpFunction {
                Objects.requireNonNull(client, "Client cannot be null");
                Objects.requireNonNull(baseUrl, "Base URL cannot be null");
                Objects.requireNonNull(urlBuilder, "URL builder cannot be null");
            }
            
            @Override
            public HttpFunctionBuilder0 path(String pathSegments) {
                Objects.requireNonNull(pathSegments, "Path segments cannot be null");
                var segments = List.of(pathSegments.split("/"));
                return new org.pragmatica.net.http.impl.HttpFunctionBuilder0Impl(client, baseUrl, segments, urlBuilder);
            }
        };
        
        return new httpFunction(this, baseUrl, UrlBuilder.create());
    }
}