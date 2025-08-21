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

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Functions.Fn10;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;

/// Function-style HTTP DSL for reusable endpoint definitions.
/// Best for: API clients, repeated calls with different parameters, type-safe parameter handling.
///
/// Example usage:
/// {@code
/// // Define reusable endpoint (no request body)
/// Fn1<Promise<User>, UserId> getUser = 
///     client.function("https://api.example.com")
///           .path("api/v1/users").pathVar(UserId.class)
///           .send(APPLICATION_JSON)
///           .get(User.class)
///           .as(APPLICATION_JSON);
///
/// // Define endpoint with request body
/// Fn2<Promise<User>, UserId, UpdateUserRequest> updateUser = 
///     client.function("https://api.example.com")
///           .path("api/v1/users").pathVar(UserId.class)
///           .send(APPLICATION_JSON, UpdateUserRequest.class)
///           .put(User.class)
///           .as(APPLICATION_JSON);
///
/// // Use multiple times with different parameters
/// var user1 = getUser.apply(new UserId("123")).await();
/// var updatedUser = updateUser.apply(new UserId("123"), updateRequest).await();
/// }
public interface HttpFunction {
    
    /// Start building a function-style endpoint with path segments
    HttpFunctionBuilder0 path(String pathSegments);
    
    /// Function builder with no path variables - path building and content type bridge
    interface HttpFunctionBuilder0 {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder0 path(String pathSegments);
        
        /// Add a typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(Class<T1> type);
        
        /// Add a generic typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type);
        
        // === Client Configuration ===
        
        /// Set connection timeout
        HttpFunctionBuilder0 connectTimeout(org.pragmatica.lang.io.TimeSpan timeout);
        
        /// Set request timeout
        HttpFunctionBuilder0 requestTimeout(org.pragmatica.lang.io.TimeSpan timeout);
        
        /// Set read timeout
        HttpFunctionBuilder0 readTimeout(org.pragmatica.lang.io.TimeSpan timeout);
        
        /// Set user agent
        HttpFunctionBuilder0 userAgent(String userAgent);
        
        /// Add default header
        HttpFunctionBuilder0 header(String name, String value);
        
        /// Set default headers
        HttpFunctionBuilder0 headers(HttpHeaders headers);
        
        /// Set max connections
        HttpFunctionBuilder0 maxConnections(int maxConnections);
        
        /// Set max connections per host
        HttpFunctionBuilder0 maxConnectionsPerHost(int maxConnectionsPerHost);
        
        /// Set follow redirects
        HttpFunctionBuilder0 followRedirects(boolean followRedirects);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder0 send(ContentType requestContentType);
        
        /// Send JSON request with no body - convenience method
        default HttpMethodBuilder0 sendJson() {
            return send(CommonContentTypes.APPLICATION_JSON);
        }
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send JSON request with body - convenience method
        default <T> HttpMethodBuilderWithBody0<T> sendJson(Class<T> bodyType) {
            return send(CommonContentTypes.APPLICATION_JSON, bodyType);
        }
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, TypeToken<T> bodyType);
        
        /// Send JSON request with body using TypeToken - convenience method
        default <T> HttpMethodBuilderWithBody0<T> sendJson(TypeToken<T> bodyType) {
            return send(CommonContentTypes.APPLICATION_JSON, bodyType);
        }
    }
    
    /// Function builder with one path variable - path building + content type bridge
    interface HttpFunctionBuilder1<T1> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder1<T1> path(String pathSegments);
        
        /// Add another typed path variable
        <T2> HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type);
        
        /// Add another generic typed path variable
        <T2> HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder1<T1> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with two path variables - path building + content type bridge
    interface HttpFunctionBuilder2<T1, T2> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder2<T1, T2> path(String pathSegments);
        
        /// Add another typed path variable
        <T3> HttpFunctionBuilder3<T1, T2, T3> pathVar(Class<T3> type);
        
        /// Add another generic typed path variable
        <T3> HttpFunctionBuilder3<T1, T2, T3> pathVar(TypeToken<T3> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder2<T1, T2> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody2<T1, T2, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody2<T1, T2, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with three path variables - path building + content type bridge
    interface HttpFunctionBuilder3<T1, T2, T3> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder3<T1, T2, T3> path(String pathSegments);
        
        /// Add another typed path variable
        <T4> HttpFunctionBuilder4<T1, T2, T3, T4> pathVar(Class<T4> type);
        
        /// Add another generic typed path variable
        <T4> HttpFunctionBuilder4<T1, T2, T3, T4> pathVar(TypeToken<T4> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder3<T1, T2, T3> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody3<T1, T2, T3, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody3<T1, T2, T3, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with four path variables - path building + content type bridge
    interface HttpFunctionBuilder4<T1, T2, T3, T4> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder4<T1, T2, T3, T4> path(String pathSegments);
        
        /// Add another typed path variable
        <T5> HttpFunctionBuilder5<T1, T2, T3, T4, T5> pathVar(Class<T5> type);
        
        /// Add another generic typed path variable
        <T5> HttpFunctionBuilder5<T1, T2, T3, T4, T5> pathVar(TypeToken<T5> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder4<T1, T2, T3, T4> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody4<T1, T2, T3, T4, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody4<T1, T2, T3, T4, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with five path variables - path building + content type bridge
    interface HttpFunctionBuilder5<T1, T2, T3, T4, T5> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder5<T1, T2, T3, T4, T5> path(String pathSegments);
        
        /// Add another typed path variable
        <T6> HttpFunctionBuilder6<T1, T2, T3, T4, T5, T6> pathVar(Class<T6> type);
        
        /// Add another generic typed path variable
        <T6> HttpFunctionBuilder6<T1, T2, T3, T4, T5, T6> pathVar(TypeToken<T6> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder5<T1, T2, T3, T4, T5> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody5<T1, T2, T3, T4, T5, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody5<T1, T2, T3, T4, T5, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with six path variables - path building + content type bridge
    interface HttpFunctionBuilder6<T1, T2, T3, T4, T5, T6> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder6<T1, T2, T3, T4, T5, T6> path(String pathSegments);
        
        /// Add another typed path variable
        <T7> HttpFunctionBuilder7<T1, T2, T3, T4, T5, T6, T7> pathVar(Class<T7> type);
        
        /// Add another generic typed path variable
        <T7> HttpFunctionBuilder7<T1, T2, T3, T4, T5, T6, T7> pathVar(TypeToken<T7> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder6<T1, T2, T3, T4, T5, T6> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody6<T1, T2, T3, T4, T5, T6, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody6<T1, T2, T3, T4, T5, T6, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with seven path variables - path building + content type bridge
    interface HttpFunctionBuilder7<T1, T2, T3, T4, T5, T6, T7> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder7<T1, T2, T3, T4, T5, T6, T7> path(String pathSegments);
        
        /// Add another typed path variable
        <T8> HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> pathVar(Class<T8> type);
        
        /// Add another generic typed path variable
        <T8> HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> pathVar(TypeToken<T8> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder7<T1, T2, T3, T4, T5, T6, T7> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with eight path variables - path building + content type bridge
    interface HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> path(String pathSegments);
        
        /// Add another typed path variable
        <T9> HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> pathVar(Class<T9> type);
        
        /// Add another generic typed path variable
        <T9> HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> pathVar(TypeToken<T9> type);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    /// Function builder with nine path variables - path building + content type bridge
    interface HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> path(String pathSegments);
        
        // === Content Type Bridge Methods ===
        
        /// Send request with no body - specify request content type for Accept header
        HttpMethodBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> send(ContentType requestContentType);
        
        /// Send request with body - specify request content type and body type
        <T> HttpMethodBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T> send(ContentType requestContentType, Class<T> bodyType);
        
        /// Send request with body using TypeToken - specify request content type and body type
        <T> HttpMethodBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T> send(ContentType requestContentType, TypeToken<T> bodyType);
    }
    
    // === HTTP Method Builders ===
    // These handle HTTP method selection and return response content type builders
    
    /// HTTP method builder for no-body requests (GET, DELETE)
    interface HttpMethodBuilder0 {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder0<R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder0<R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder0<Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder0<R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder0<R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder0<Unit> delete();
    }
    
    /// HTTP method function builder for no path variables
    interface HttpMethodFunctionBuilder0 {
        
        /// Create a function for the specified HTTP method with typed response and body
        <R, U> Fn1<Promise<R>, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType);
        
        /// Create a function for the specified HTTP method with typed response and body using TypeToken
        <R, U> Fn1<Promise<R>, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType);
    }
    
    /// HTTP method builder for one path variable, no-body requests
    interface HttpMethodBuilder1<T1> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder1<T1, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder1<T1, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder1<T1, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder1<T1, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder1<T1, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder1<T1, Unit> delete();
    }
    
    /// HTTP method function builder for one path variable
    interface HttpMethodFunctionBuilder1<T1> {
        
        /// Create a function for the specified HTTP method with typed response and body
        <R, U> Fn2<Promise<R>, T1, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType);
        
        /// Create a function for the specified HTTP method with typed response and body using TypeToken
        <R, U> Fn2<Promise<R>, T1, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType);
    }
    
    /// HTTP method builder for two path variables, no-body requests
    interface HttpMethodBuilder2<T1, T2> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder2<T1, T2, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder2<T1, T2, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder2<T1, T2, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder2<T1, T2, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder2<T1, T2, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder2<T1, T2, Unit> delete();
    }
    
    /// HTTP method function builder for two path variables
    interface HttpMethodFunctionBuilder2<T1, T2> {
        
        /// Create a function for the specified HTTP method with typed response and body
        <R, U> Fn3<Promise<R>, T1, T2, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType);
        
        /// Create a function for the specified HTTP method with typed response and body using TypeToken
        <R, U> Fn3<Promise<R>, T1, T2, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType);
    }
    
    /// HTTP method builder for three path variables, no-body requests
    interface HttpMethodBuilder3<T1, T2, T3> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder3<T1, T2, T3, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder3<T1, T2, T3, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder3<T1, T2, T3, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder3<T1, T2, T3, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder3<T1, T2, T3, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder3<T1, T2, T3, Unit> delete();
    }
    
    /// HTTP method function builder for three path variables
    interface HttpMethodFunctionBuilder3<T1, T2, T3> {
        
        /// Create a function for the specified HTTP method with typed response and body
        <R, U> Fn4<Promise<R>, T1, T2, T3, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType);
        
        /// Create a function for the specified HTTP method with typed response and body using TypeToken
        <R, U> Fn4<Promise<R>, T1, T2, T3, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType);
    }
    
    /// HTTP method builder for four path variables, no-body requests
    interface HttpMethodBuilder4<T1, T2, T3, T4> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder4<T1, T2, T3, T4, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder4<T1, T2, T3, T4, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder4<T1, T2, T3, T4, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder4<T1, T2, T3, T4, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder4<T1, T2, T3, T4, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder4<T1, T2, T3, T4, Unit> delete();
    }
    
    /// HTTP method builder for five path variables, no-body requests
    interface HttpMethodBuilder5<T1, T2, T3, T4, T5> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, Unit> delete();
    }
    
    /// HTTP method builder for six path variables, no-body requests
    interface HttpMethodBuilder6<T1, T2, T3, T4, T5, T6> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, Unit> delete();
    }
    
    /// HTTP method builder for seven path variables, no-body requests
    interface HttpMethodBuilder7<T1, T2, T3, T4, T5, T6, T7> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, Unit> delete();
    }
    
    /// HTTP method builder for eight path variables, no-body requests
    interface HttpMethodBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, Unit> delete();
    }
    
    /// HTTP method builder for nine path variables, no-body requests
    interface HttpMethodBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
        
        /// Create GET method - returns response content type builder
        <R> HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> get(Class<R> responseType);
        
        /// Create GET method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> get(TypeToken<R> responseType);
        
        /// Create GET method, no response body expected
        HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Unit> get();
        
        /// Create DELETE method - returns response content type builder
        <R> HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> delete(Class<R> responseType);
        
        /// Create DELETE method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> delete(TypeToken<R> responseType);
        
        /// Create DELETE method, no response body expected
        HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Unit> delete();
    }
    
    // === HTTP Method Builders with Body ===
    // These handle HTTP method selection for requests with body
    
    /// HTTP method builder for body requests (POST, PUT, PATCH)
    interface HttpMethodBuilderWithBody0<B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody0<B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody0<B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody0<B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody0<B, Unit> patch();
    }
    
    /// HTTP method builder for one path variable with body requests
    interface HttpMethodBuilderWithBody1<T1, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody1<T1, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> patch();
    }
    
    /// HTTP method builder for two path variables with body requests
    interface HttpMethodBuilderWithBody2<T1, T2, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> patch();
    }
    
    /// HTTP method builder for three path variables with body requests
    interface HttpMethodBuilderWithBody3<T1, T2, T3, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> patch();
    }
    
    /// HTTP method builder for four path variables with body requests
    interface HttpMethodBuilderWithBody4<T1, T2, T3, T4, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, Unit> patch();
    }
    
    /// HTTP method builder for five path variables with body requests
    interface HttpMethodBuilderWithBody5<T1, T2, T3, T4, T5, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, Unit> patch();
    }
    
    /// HTTP method builder for six path variables with body requests
    interface HttpMethodBuilderWithBody6<T1, T2, T3, T4, T5, T6, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, Unit> patch();
    }
    
    /// HTTP method builder for seven path variables with body requests
    interface HttpMethodBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, Unit> patch();
    }
    
    /// HTTP method builder for eight path variables with body requests
    interface HttpMethodBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, Unit> patch();
    }
    
    /// HTTP method builder for nine path variables with body requests
    interface HttpMethodBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B> {
        
        /// Create POST method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> post(Class<R> responseType);
        
        /// Create POST method with generic response type - returns response content type builder  
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> post(TypeToken<R> responseType);
        
        /// Create POST method, no response body expected
        HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, Unit> post();
        
        /// Create PUT method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> put(Class<R> responseType);
        
        /// Create PUT method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> put(TypeToken<R> responseType);
        
        /// Create PUT method, no response body expected
        HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, Unit> put();
        
        /// Create PATCH method - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> patch(Class<R> responseType);
        
        /// Create PATCH method with generic response type - returns response content type builder
        <R> HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> patch(TypeToken<R> responseType);
        
        /// Create PATCH method, no response body expected
        HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, Unit> patch();
    }
    
    // === Response Content Type Builders ===
    // These handle the final .as(ContentType) step and return the actual function
    
    /// Response content type builder for no path variables
    interface HttpResponseContentTypeBuilder0<R> {
        
        /// Specify response content type and create final function
        Fn0<Promise<R>> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn0<Promise<R>> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for one path variable
    interface HttpResponseContentTypeBuilder1<T1, R> {
        
        /// Specify response content type and create final function
        Fn1<Promise<R>, T1> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn1<Promise<R>, T1> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for two path variables
    interface HttpResponseContentTypeBuilder2<T1, T2, R> {
        
        /// Specify response content type and create final function
        Fn2<Promise<R>, T1, T2> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn2<Promise<R>, T1, T2> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for three path variables
    interface HttpResponseContentTypeBuilder3<T1, T2, T3, R> {
        
        /// Specify response content type and create final function
        Fn3<Promise<R>, T1, T2, T3> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn3<Promise<R>, T1, T2, T3> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for four path variables
    interface HttpResponseContentTypeBuilder4<T1, T2, T3, T4, R> {
        
        /// Specify response content type and create final function
        Fn4<Promise<R>, T1, T2, T3, T4> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn4<Promise<R>, T1, T2, T3, T4> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for five path variables
    interface HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> {
        
        /// Specify response content type and create final function
        Fn5<Promise<R>, T1, T2, T3, T4, T5> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn5<Promise<R>, T1, T2, T3, T4, T5> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for six path variables
    interface HttpResponseContentTypeBuilder6<T1, T2, T3, T4, T5, T6, R> {
        
        /// Specify response content type and create final function
        Fn6<Promise<R>, T1, T2, T3, T4, T5, T6> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn6<Promise<R>, T1, T2, T3, T4, T5, T6> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for seven path variables
    interface HttpResponseContentTypeBuilder7<T1, T2, T3, T4, T5, T6, T7, R> {
        
        /// Specify response content type and create final function
        Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, T7> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for eight path variables
    interface HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> {
        
        /// Specify response content type and create final function
        Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for nine path variables
    interface HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {
        
        /// Specify response content type and create final function
        Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    // === Response Content Type Builders with Body ===
    // These handle the final .as(ContentType) step for requests with body and return the actual function
    
    /// Response content type builder for no path variables with body
    interface HttpResponseContentTypeBuilderWithBody0<B, R> {
        
        /// Specify response content type and create final function
        Fn1<Promise<R>, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn1<Promise<R>, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for one path variable with body
    interface HttpResponseContentTypeBuilderWithBody1<T1, B, R> {
        
        /// Specify response content type and create final function
        Fn2<Promise<R>, T1, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn2<Promise<R>, T1, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for two path variables with body
    interface HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> {
        
        /// Specify response content type and create final function
        Fn3<Promise<R>, T1, T2, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn3<Promise<R>, T1, T2, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for three path variables with body
    interface HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> {
        
        /// Specify response content type and create final function
        Fn4<Promise<R>, T1, T2, T3, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn4<Promise<R>, T1, T2, T3, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for four path variables with body
    interface HttpResponseContentTypeBuilderWithBody4<T1, T2, T3, T4, B, R> {
        
        /// Specify response content type and create final function
        Fn5<Promise<R>, T1, T2, T3, T4, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn5<Promise<R>, T1, T2, T3, T4, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for five path variables with body
    interface HttpResponseContentTypeBuilderWithBody5<T1, T2, T3, T4, T5, B, R> {
        
        /// Specify response content type and create final function
        Fn6<Promise<R>, T1, T2, T3, T4, T5, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn6<Promise<R>, T1, T2, T3, T4, T5, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for six path variables with body
    interface HttpResponseContentTypeBuilderWithBody6<T1, T2, T3, T4, T5, T6, B, R> {
        
        /// Specify response content type and create final function
        Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn7<Promise<R>, T1, T2, T3, T4, T5, T6, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for seven path variables with body
    interface HttpResponseContentTypeBuilderWithBody7<T1, T2, T3, T4, T5, T6, T7, B, R> {
        
        /// Specify response content type and create final function
        Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn8<Promise<R>, T1, T2, T3, T4, T5, T6, T7, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for eight path variables with body
    interface HttpResponseContentTypeBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, B, R> {
        
        /// Specify response content type and create final function
        Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
    
    /// Response content type builder for nine path variables with body
    interface HttpResponseContentTypeBuilderWithBody9<T1, T2, T3, T4, T5, T6, T7, T8, T9, B, R> {
        
        /// Specify response content type and create final function
        Fn10<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, B> as(ContentType responseContentType);
        
        /// JSON response convenience method
        default Fn10<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9, B> asJson() {
            return as(CommonContentTypes.APPLICATION_JSON);
        }
    }
}