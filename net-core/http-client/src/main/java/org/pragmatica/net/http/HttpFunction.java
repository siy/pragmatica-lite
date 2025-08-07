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
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;

/// Function-style HTTP DSL for reusable endpoint definitions.
/// Best for: API clients, repeated calls with different parameters, type-safe parameter handling.
///
/// Example usage:
/// {@code
/// // Define reusable endpoint
/// Fn1<Promise<User>, UserId> getUser = 
///     client.function("https://api.example.com")
///           .path("api/v1/users").pathVar(UserId.class)
///           .json()
///           .get(User.class);
///
/// // Use multiple times with different parameters
/// var user1 = getUser.apply(new UserId("123")).await();
/// var user2 = getUser.apply(new UserId("456")).await();
/// }
public interface HttpFunction {
    
    /// Start building a function-style endpoint with path segments
    HttpFunctionBuilder0 path(String pathSegments);
    
    /// Function builder with no path variables - only path building methods
    interface HttpFunctionBuilder0 {
        
        // === Path Building Only ===
        
        /// Add more path segments
        HttpFunctionBuilder0 path(String pathSegments);
        
        /// Add a typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(Class<T1> type);
        
        /// Add a generic typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type);
        
        // === Content Type Bridge Methods ===
        
        /// Use JSON content type for request/response
        HttpMethodFunctionBuilder0 json();
        
        /// Use JSON with specific content type
        HttpMethodFunctionBuilder0 json(String contentType);
        
        /// Use plain text content type
        HttpMethodFunctionBuilder0 plainText();
        
        /// Use plain text with specific content type  
        HttpMethodFunctionBuilder0 plainText(String contentType);
        
        /// Use custom content type
        HttpMethodFunctionBuilder0 contentType(String contentType);
        
        /// Use ContentType interface
        HttpMethodFunctionBuilder0 contentType(ContentType contentType);
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
        
        /// Use JSON content type for request/response
        HttpMethodFunctionBuilder1<T1> json();
        
        /// Use JSON with specific content type
        HttpMethodFunctionBuilder1<T1> json(String contentType);
        
        /// Use plain text content type
        HttpMethodFunctionBuilder1<T1> plainText();
        
        /// Use plain text with specific content type  
        HttpMethodFunctionBuilder1<T1> plainText(String contentType);
        
        /// Use custom content type
        HttpMethodFunctionBuilder1<T1> contentType(String contentType);
        
        /// Use ContentType interface
        HttpMethodFunctionBuilder1<T1> contentType(ContentType contentType);
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
        
        /// Use JSON content type for request/response
        HttpMethodFunctionBuilder2<T1, T2> json();
        
        /// Use JSON with specific content type
        HttpMethodFunctionBuilder2<T1, T2> json(String contentType);
        
        /// Use plain text content type
        HttpMethodFunctionBuilder2<T1, T2> plainText();
        
        /// Use plain text with specific content type  
        HttpMethodFunctionBuilder2<T1, T2> plainText(String contentType);
        
        /// Use custom content type
        HttpMethodFunctionBuilder2<T1, T2> contentType(String contentType);
        
        /// Use ContentType interface
        HttpMethodFunctionBuilder2<T1, T2> contentType(ContentType contentType);
    }
    
    /// Function builder with three path variables - path building + content type bridge
    interface HttpFunctionBuilder3<T1, T2, T3> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder3<T1, T2, T3> path(String pathSegments);
        
        // === Content Type Bridge Methods ===
        
        /// Use JSON content type for request/response
        HttpMethodFunctionBuilder3<T1, T2, T3> json();
        
        /// Use JSON with specific content type
        HttpMethodFunctionBuilder3<T1, T2, T3> json(String contentType);
        
        /// Use plain text content type
        HttpMethodFunctionBuilder3<T1, T2, T3> plainText();
        
        /// Use plain text with specific content type  
        HttpMethodFunctionBuilder3<T1, T2, T3> plainText(String contentType);
        
        /// Use custom content type
        HttpMethodFunctionBuilder3<T1, T2, T3> contentType(String contentType);
        
        /// Use ContentType interface
        HttpMethodFunctionBuilder3<T1, T2, T3> contentType(ContentType contentType);
    }
    
    // === HTTP Method Function Builders ===
    // These return Promise<R> (clean, no HTTP specifics)
    
    /// HTTP method builder with no path variables
    interface HttpMethodFunctionBuilder0 {
        
        // === Core Terminal Operations ===
        
        /// Create function for HTTP method with typed response (no body)
        <R> Fn0<Promise<R>> method(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with generic response type (no body)
        <R> Fn0<Promise<R>> method(HttpMethod method, TypeToken<R> responseType);
        
        // === Convenience Methods (Default Implementations) ===
        
        /// Create GET function with typed response
        default <R> Fn0<Promise<R>> get(Class<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        /// Create GET function with generic response type
        default <R> Fn0<Promise<R>> get(TypeToken<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        /// Create GET function, no response body expected
        default Fn0<Promise<Unit>> get() {
            return method(HttpMethod.GET, Unit.class);
        }
        
        /// Create DELETE function with typed response
        default <R> Fn0<Promise<R>> delete(Class<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        /// Create DELETE function with generic response type
        default <R> Fn0<Promise<R>> delete(TypeToken<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        /// Create DELETE function, no response body expected
        default Fn0<Promise<Unit>> delete() {
            return method(HttpMethod.DELETE, Unit.class);
        }
    }
    
    /// HTTP method builder with one path variable
    interface HttpMethodFunctionBuilder1<T1> {
        
        // === Core Terminal Operations ===
        
        /// Create function for HTTP method with one path parameter (no body)
        <R> Fn1<Promise<R>, T1> method(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with one path parameter and generic response (no body)
        <R> Fn1<Promise<R>, T1> method(HttpMethod method, TypeToken<R> responseType);
        
        /// Create function for HTTP method with path parameter and body
        <R> Fn2<Promise<R>, T1, Object> methodWithBody(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with path parameter, body, and generic response
        <R> Fn2<Promise<R>, T1, Object> methodWithBody(HttpMethod method, TypeToken<R> responseType);
        
        // === Convenience Methods (Default Implementations) ===
        
        /// Create GET function with one path parameter
        default <R> Fn1<Promise<R>, T1> get(Class<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        /// Create GET function with one path parameter and generic response type
        default <R> Fn1<Promise<R>, T1> get(TypeToken<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        /// Create GET function with one path parameter, no response body expected
        default Fn1<Promise<Unit>, T1> get() {
            return method(HttpMethod.GET, Unit.class);
        }
        
        /// Create POST function with path parameter and body
        default <R> Fn2<Promise<R>, T1, Object> post(Class<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        /// Create POST function with path parameter, body, and generic response type
        default <R> Fn2<Promise<R>, T1, Object> post(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        /// Create POST function with path parameter and body, no response body expected
        default Fn2<Promise<Unit>, T1, Object> post() {
            return methodWithBody(HttpMethod.POST, Unit.class);
        }
        
        /// Create PUT function with path parameter and body
        default <R> Fn2<Promise<R>, T1, Object> put(Class<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        /// Create PUT function with path parameter, body, and generic response type
        default <R> Fn2<Promise<R>, T1, Object> put(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        /// Create PUT function with path parameter and body, no response body expected
        default Fn2<Promise<Unit>, T1, Object> put() {
            return methodWithBody(HttpMethod.PUT, Unit.class);
        }
        
        /// Create PATCH function with path parameter and body
        default <R> Fn2<Promise<R>, T1, Object> patch(Class<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        /// Create PATCH function with path parameter, body, and generic response type
        default <R> Fn2<Promise<R>, T1, Object> patch(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        /// Create PATCH function with path parameter and body, no response body expected
        default Fn2<Promise<Unit>, T1, Object> patch() {
            return methodWithBody(HttpMethod.PATCH, Unit.class);
        }
        
        /// Create DELETE function with one path parameter
        default <R> Fn1<Promise<R>, T1> delete(Class<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        /// Create DELETE function with one path parameter and generic response type
        default <R> Fn1<Promise<R>, T1> delete(TypeToken<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        /// Create DELETE function with one path parameter, no response body expected
        default Fn1<Promise<Unit>, T1> delete() {
            return method(HttpMethod.DELETE, Unit.class);
        }
    }
    
    /// HTTP method builder with two path variables  
    interface HttpMethodFunctionBuilder2<T1, T2> {
        
        // === Core Terminal Operations ===
        
        /// Create function for HTTP method with two path parameters (no body)
        <R> Fn2<Promise<R>, T1, T2> method(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with two path parameters and generic response (no body)
        <R> Fn2<Promise<R>, T1, T2> method(HttpMethod method, TypeToken<R> responseType);
        
        /// Create function for HTTP method with two path parameters and body
        <R> Fn3<Promise<R>, T1, T2, Object> methodWithBody(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with two path parameters, body, and generic response
        <R> Fn3<Promise<R>, T1, T2, Object> methodWithBody(HttpMethod method, TypeToken<R> responseType);
        
        // === Convenience Methods (Default Implementations) ===
        
        default <R> Fn2<Promise<R>, T1, T2> get(Class<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        default <R> Fn2<Promise<R>, T1, T2> get(TypeToken<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        default Fn2<Promise<Unit>, T1, T2> get() {
            return method(HttpMethod.GET, Unit.class);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> post(Class<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> post(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        default Fn3<Promise<Unit>, T1, T2, Object> post() {
            return methodWithBody(HttpMethod.POST, Unit.class);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> put(Class<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> put(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        default Fn3<Promise<Unit>, T1, T2, Object> put() {
            return methodWithBody(HttpMethod.PUT, Unit.class);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> patch(Class<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, Object> patch(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        default Fn3<Promise<Unit>, T1, T2, Object> patch() {
            return methodWithBody(HttpMethod.PATCH, Unit.class);
        }
        
        default <R> Fn2<Promise<R>, T1, T2> delete(Class<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        default <R> Fn2<Promise<R>, T1, T2> delete(TypeToken<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        default Fn2<Promise<Unit>, T1, T2> delete() {
            return method(HttpMethod.DELETE, Unit.class);
        }
    }
    
    /// HTTP method builder with three path variables
    interface HttpMethodFunctionBuilder3<T1, T2, T3> {
        
        // === Core Terminal Operations ===
        
        /// Create function for HTTP method with three path parameters (no body)
        <R> Fn3<Promise<R>, T1, T2, T3> method(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with three path parameters and generic response (no body)
        <R> Fn3<Promise<R>, T1, T2, T3> method(HttpMethod method, TypeToken<R> responseType);
        
        /// Create function for HTTP method with three path parameters and body
        <R> Fn4<Promise<R>, T1, T2, T3, Object> methodWithBody(HttpMethod method, Class<R> responseType);
        
        /// Create function for HTTP method with three path parameters, body, and generic response
        <R> Fn4<Promise<R>, T1, T2, T3, Object> methodWithBody(HttpMethod method, TypeToken<R> responseType);
        
        // === Convenience Methods (Default Implementations) ===
        
        default <R> Fn3<Promise<R>, T1, T2, T3> get(Class<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, T3> get(TypeToken<R> responseType) {
            return method(HttpMethod.GET, responseType);
        }
        
        default Fn3<Promise<Unit>, T1, T2, T3> get() {
            return method(HttpMethod.GET, Unit.class);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> post(Class<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> post(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.POST, responseType);
        }
        
        default Fn4<Promise<Unit>, T1, T2, T3, Object> post() {
            return methodWithBody(HttpMethod.POST, Unit.class);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> put(Class<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> put(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PUT, responseType);
        }
        
        default Fn4<Promise<Unit>, T1, T2, T3, Object> put() {
            return methodWithBody(HttpMethod.PUT, Unit.class);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> patch(Class<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        default <R> Fn4<Promise<R>, T1, T2, T3, Object> patch(TypeToken<R> responseType) {
            return methodWithBody(HttpMethod.PATCH, responseType);
        }
        
        default Fn4<Promise<Unit>, T1, T2, T3, Object> patch() {
            return methodWithBody(HttpMethod.PATCH, Unit.class);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, T3> delete(Class<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        default <R> Fn3<Promise<R>, T1, T2, T3> delete(TypeToken<R> responseType) {
            return method(HttpMethod.DELETE, responseType);
        }
        
        default Fn3<Promise<Unit>, T1, T2, T3> delete() {
            return method(HttpMethod.DELETE, Unit.class);
        }
    }
}