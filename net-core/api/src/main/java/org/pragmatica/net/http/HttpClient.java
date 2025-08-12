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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    default HttpResource resource(String baseUrl) {
        return resourceImpl(baseUrl);
    }
    
    /// Function-style DSL for reusable endpoint definitions
    /// Best for: API clients, repeated calls with different parameters, type-safe parameter handling
    default HttpFunction function(String baseUrl) {
        return functionImpl(baseUrl);
    }
    
    /// Template-style request builder
    /// Best for: complex URLs, migration from existing string-based APIs, dynamic URL construction
    HttpRequestBuilder request();
    
    // === Direct Template Methods (delegated to HttpRequestBuilder) ===
    
    /// Direct GET with URL template
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, Class<R> responseType) {
        return request().get(urlTemplate, params, responseType);
    }
    
    /// Direct GET with URL template and TypeToken
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, TypeToken<R> responseType) {
        return request().get(urlTemplate, params, responseType);
    }
    
    /// Direct GET with URL template, no response body expected
    default Promise<HttpResponse<Unit>> get(String urlTemplate, Object[] params) {
        return request().get(urlTemplate, params);
    }
    
    /// Direct POST with URL template
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, Class<R> responseType) {
        return request().post(urlTemplate, params, body, responseType);
    }
    
    /// Direct POST with URL template and TypeToken
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, TypeToken<R> responseType) {
        return request().post(urlTemplate, params, body, responseType);
    }
    
    /// Direct POST with URL template, no response body expected
    default Promise<HttpResponse<Unit>> post(String urlTemplate, Object[] params, Object body) {
        return request().post(urlTemplate, params, body);
    }
    
    // === Direct Template Methods with Content Type ===
    
    /// Direct GET with URL template and content type
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, String contentType, Class<R> responseType) {
        return request().get(urlTemplate, params, contentType, responseType);
    }
    
    /// Direct GET with URL template, content type and TypeToken
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, String contentType, TypeToken<R> responseType) {
        return request().get(urlTemplate, params, contentType, responseType);
    }
    
    /// Direct POST with URL template and content type
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, String contentType, Class<R> responseType) {
        return request().post(urlTemplate, params, body, contentType, responseType);
    }
    
    /// Direct POST with URL template, content type and TypeToken
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, String contentType, TypeToken<R> responseType) {
        return request().post(urlTemplate, params, body, contentType, responseType);
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
    
    // === Default Implementations ===
    
    default HttpResource resourceImpl(String baseUrl) {
        
        record httpResource(HttpClient client, String baseUrl, 
                           List<String> pathSegments, Map<String, String> queryParams,
                           HttpHeaders headers, UrlBuilder urlBuilder, String contentType) implements HttpResource {
            
            httpResource {
                // contentType can be null (no content type set)
            }
            
            @Override
            public HttpResource path(String pathSegments) {
                var segments = pathSegments.split("/");
                var newSegments = new ArrayList<>(this.pathSegments);
                for (var segment : segments) {
                    if (!segment.isEmpty()) {
                        newSegments.add(segment);
                    }
                }
                return new httpResource(client, baseUrl, newSegments, queryParams, headers, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource pathVar(String name, String value) {
                return path(value);
            }
            
            @Override
            public HttpResource pathTemplate(String template, Object... args) {
                var resolvedPath = urlBuilder.resolveTemplate(template, args);
                return path(resolvedPath);
            }
            
            @Override
            public HttpResource queryParam(String name, String value) {
                var newParams = new HashMap<>(queryParams);
                newParams.put(name, value);
                return new httpResource(client, baseUrl, pathSegments, newParams, headers, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource header(String name, String value) {
                var newHeaders = new HttpHeaders();
                // Copy existing headers
                for (var headerName : headers.names()) {
                    for (var headerValue : headers.all(headerName)) {
                        newHeaders.add(headerName, headerValue);
                    }
                }
                newHeaders.add(name, value);
                return new httpResource(client, baseUrl, pathSegments, queryParams, newHeaders, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource headers(HttpHeaders headers) {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource json() {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, "application/json");
            }
            
            @Override
            public HttpResource json(String contentType) {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource plainText() {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, "text/plain");
            }
            
            @Override
            public HttpResource plainText(String contentType) {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, contentType);
            }
            
            @Override
            public HttpResource contentType(String contentType) {
                return new httpResource(client, baseUrl, pathSegments, queryParams, headers, urlBuilder, contentType);
            }
            
            private HttpRequestBuilder prepareRequest() {
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                var requestBuilder = client.request().url(url).headers(headers);
                if (contentType != null) {
                    requestBuilder = requestBuilder.header("Content-Type", contentType);
                }
                return requestBuilder;
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> get(Class<T> responseType) {
                return prepareRequest().get(responseType);
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> get(TypeToken<T> responseType) {
                return prepareRequest().get(responseType);
            }
            
            @Override
            public Promise<HttpResponse<Unit>> get() {
                return prepareRequest().get(Unit.class);
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> post(Object body, Class<T> responseType) {
                return prepareRequest().post(body, responseType);
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> post(Object body, TypeToken<T> responseType) {
                return prepareRequest().post(body, responseType);
            }
            
            @Override
            public Promise<HttpResponse<Unit>> post(Object body) {
                return prepareRequest().method(HttpMethod.POST).body(body).responseType(Unit.class).send();
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> put(Object body, Class<T> responseType) {
                return prepareRequest().put(body, responseType);
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> put(Object body, TypeToken<T> responseType) {
                return prepareRequest().put(body, responseType);
            }
            
            @Override
            public Promise<HttpResponse<Unit>> put(Object body) {
                return prepareRequest().method(HttpMethod.PUT).body(body).responseType(Unit.class).send();
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> patch(Object body, Class<T> responseType) {
                return prepareRequest().body(body).method(HttpMethod.PATCH).responseType(responseType).send();
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> patch(Object body, TypeToken<T> responseType) {
                return prepareRequest().body(body).method(HttpMethod.PATCH).responseType(responseType).send();
            }
            
            @Override
            public Promise<HttpResponse<Unit>> patch(Object body) {
                return prepareRequest().body(body).method(HttpMethod.PATCH).responseType(Unit.class).send();
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> delete(Class<T> responseType) {
                return prepareRequest().delete(responseType);
            }
            
            @Override
            public <T> Promise<HttpResponse<T>> delete(TypeToken<T> responseType) {
                return prepareRequest().delete(responseType);
            }
            
            @Override
            public Promise<HttpResponse<Unit>> delete() {
                return prepareRequest().delete(Unit.class);
            }
            
            @Override
            public HttpRequestBuilder request() {
                var url = urlBuilder.buildUrl(baseUrl, pathSegments, queryParams);
                return client.request().url(url).headers(headers);
            }
        };
        
        return new httpResource(this, baseUrl, emptyList(), emptyMap(), new HttpHeaders(), UrlBuilder.create(), null);
    }
    
    default HttpFunction functionImpl(String baseUrl) {
        
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