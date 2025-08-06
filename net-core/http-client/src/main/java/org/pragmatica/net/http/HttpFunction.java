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
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;

/// Function-style HTTP DSL for reusable endpoint definitions.
/// Best for: API clients, repeated calls with different parameters, type-safe parameter handling.
///
/// Example usage:
/// {@code
/// // Define reusable endpoint
/// Fn1<Promise<Result<HttpResponse<User>>>, UserId> getUser = 
///     client.function("https://api.example.com")
///           .path("api/v1/users").pathVar(UserId.class)
///           .get(User.class);
///
/// // Use multiple times with different parameters
/// var user1 = getUser.apply(new UserId("123")).await();
/// var user2 = getUser.apply(new UserId("456")).await();
/// }
public interface HttpFunction {
    
    /// Start building a function-style endpoint with path segments
    HttpFunctionBuilder0 path(String pathSegments);
    
    /// Function builder with no path variables
    interface HttpFunctionBuilder0 {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder0 path(String pathSegments);
        
        /// Add a typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(Class<T1> type);
        
        /// Add a generic typed path variable
        <T1> HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type);
        
        // === Terminal Operations (Return Functions) ===
        
        /// Create GET function with no parameters
        <R> Fn0<Promise<Result<HttpResponse<R>>>> get(Class<R> responseType);
        
        /// Create GET function with no parameters and generic response type
        <R> Fn0<Promise<Result<HttpResponse<R>>>> get(TypeToken<R> responseType);
        
        /// Create GET function with no parameters, no response body expected
        Fn0<Promise<Result<HttpResponse<Unit>>>> get();
        
        /// Create POST function with body parameter
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> post(Class<R> responseType);
        
        /// Create POST function with body parameter and generic response type
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> post(TypeToken<R> responseType);
        
        /// Create POST function with body parameter, no response body expected
        Fn1<Promise<Result<HttpResponse<Unit>>>, Object> post();
        
        /// Create PUT function with body parameter
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> put(Class<R> responseType);
        
        /// Create PUT function with body parameter and generic response type
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> put(TypeToken<R> responseType);
        
        /// Create PUT function with body parameter, no response body expected
        Fn1<Promise<Result<HttpResponse<Unit>>>, Object> put();
        
        /// Create PATCH function with body parameter
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> patch(Class<R> responseType);
        
        /// Create PATCH function with body parameter and generic response type
        <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> patch(TypeToken<R> responseType);
        
        /// Create PATCH function with body parameter, no response body expected
        Fn1<Promise<Result<HttpResponse<Unit>>>, Object> patch();
        
        /// Create DELETE function with no parameters
        <R> Fn0<Promise<Result<HttpResponse<R>>>> delete(Class<R> responseType);
        
        /// Create DELETE function with no parameters and generic response type
        <R> Fn0<Promise<Result<HttpResponse<R>>>> delete(TypeToken<R> responseType);
        
        /// Create DELETE function with no parameters, no response body expected
        Fn0<Promise<Result<HttpResponse<Unit>>>> delete();
    }
    
    /// Function builder with one path variable
    interface HttpFunctionBuilder1<T1> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder1<T1> path(String pathSegments);
        
        /// Add another typed path variable
        <T2> HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type);
        
        /// Add another generic typed path variable
        <T2> HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type);
        
        // === Terminal Operations (Return Functions) ===
        
        /// Create GET function with one path parameter
        <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> get(Class<R> responseType);
        
        /// Create GET function with one path parameter and generic response type
        <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> get(TypeToken<R> responseType);
        
        /// Create GET function with one path parameter, no response body expected
        Fn1<Promise<Result<HttpResponse<Unit>>>, T1> get();
        
        /// Create POST function with path parameter and body
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> post(Class<R> responseType);
        
        /// Create POST function with path parameter, body, and generic response type
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> post(TypeToken<R> responseType);
        
        /// Create POST function with path parameter and body, no response body expected
        Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> post();
        
        /// Create PUT function with path parameter and body
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> put(Class<R> responseType);
        
        /// Create PUT function with path parameter, body, and generic response type
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> put(TypeToken<R> responseType);
        
        /// Create PUT function with path parameter and body, no response body expected
        Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> put();
        
        /// Create PATCH function with path parameter and body
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> patch(Class<R> responseType);
        
        /// Create PATCH function with path parameter, body, and generic response type
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> patch(TypeToken<R> responseType);
        
        /// Create PATCH function with path parameter and body, no response body expected
        Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> patch();
        
        /// Create DELETE function with one path parameter
        <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> delete(Class<R> responseType);
        
        /// Create DELETE function with one path parameter and generic response type
        <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> delete(TypeToken<R> responseType);
        
        /// Create DELETE function with one path parameter, no response body expected
        Fn1<Promise<Result<HttpResponse<Unit>>>, T1> delete();
    }
    
    /// Function builder with two path variables
    interface HttpFunctionBuilder2<T1, T2> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder2<T1, T2> path(String pathSegments);
        
        /// Add another typed path variable
        <T3> HttpFunctionBuilder3<T1, T2, T3> pathVar(Class<T3> type);
        
        /// Add another generic typed path variable
        <T3> HttpFunctionBuilder3<T1, T2, T3> pathVar(TypeToken<T3> type);
        
        // === Terminal Operations (Return Functions) ===
        
        /// Create GET function with two path parameters
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> get(Class<R> responseType);
        
        /// Create GET function with two path parameters and generic response type
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> get(TypeToken<R> responseType);
        
        /// Create GET function with two path parameters, no response body expected
        Fn2<Promise<Result<HttpResponse<Unit>>>, T1, T2> get();
        
        /// Create POST function with two path parameters and body
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> post(Class<R> responseType);
        
        /// Create POST function with two path parameters, body, and generic response type
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> post(TypeToken<R> responseType);
        
        /// Create POST function with two path parameters and body, no response body expected
        Fn3<Promise<Result<HttpResponse<Unit>>>, T1, T2, Object> post();
        
        /// Create PUT function with two path parameters and body
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> put(Class<R> responseType);
        
        /// Create PUT function with two path parameters, body, and generic response type
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> put(TypeToken<R> responseType);
        
        /// Create PUT function with two path parameters and body, no response body expected
        Fn3<Promise<Result<HttpResponse<Unit>>>, T1, T2, Object> put();
        
        /// Create PATCH function with two path parameters and body
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> patch(Class<R> responseType);
        
        /// Create PATCH function with two path parameters, body, and generic response type
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> patch(TypeToken<R> responseType);
        
        /// Create PATCH function with two path parameters and body, no response body expected
        Fn3<Promise<Result<HttpResponse<Unit>>>, T1, T2, Object> patch();
        
        /// Create DELETE function with two path parameters
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> delete(Class<R> responseType);
        
        /// Create DELETE function with two path parameters and generic response type
        <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> delete(TypeToken<R> responseType);
        
        /// Create DELETE function with two path parameters, no response body expected
        Fn2<Promise<Result<HttpResponse<Unit>>>, T1, T2> delete();
    }
    
    /// Function builder with three path variables
    interface HttpFunctionBuilder3<T1, T2, T3> {
        
        // === Path Building ===
        
        /// Add more path segments
        HttpFunctionBuilder3<T1, T2, T3> path(String pathSegments);
        
        // === Terminal Operations (Return Functions) ===
        
        /// Create GET function with three path parameters
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, T3> get(Class<R> responseType);
        
        /// Create GET function with three path parameters and generic response type
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, T3> get(TypeToken<R> responseType);
        
        /// Create GET function with three path parameters, no response body expected
        Fn3<Promise<Result<HttpResponse<Unit>>>, T1, T2, T3> get();
        
        /// Create POST function with three path parameters and body
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> post(Class<R> responseType);
        
        /// Create POST function with three path parameters, body, and generic response type
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> post(TypeToken<R> responseType);
        
        /// Create POST function with three path parameters and body, no response body expected
        Fn4<Promise<Result<HttpResponse<Unit>>>, T1, T2, T3, Object> post();
        
        /// Create PUT function with three path parameters and body
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> put(Class<R> responseType);
        
        /// Create PUT function with three path parameters, body, and generic response type
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> put(TypeToken<R> responseType);
        
        /// Create PUT function with three path parameters and body, no response body expected
        Fn4<Promise<Result<HttpResponse<Unit>>>, T1, T2, T3, Object> put();
        
        /// Create PATCH function with three path parameters and body
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> patch(Class<R> responseType);
        
        /// Create PATCH function with three path parameters, body, and generic response type
        <R> Fn4<Promise<Result<HttpResponse<R>>>, T1, T2, T3, Object> patch(TypeToken<R> responseType);
        
        /// Create PATCH function with three path parameters and body, no response body expected
        Fn4<Promise<Result<HttpResponse<Unit>>>, T1, T2, T3, Object> patch();
        
        /// Create DELETE function with three path parameters
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, T3> delete(Class<R> responseType);
        
        /// Create DELETE function with three path parameters and generic response type
        <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, T3> delete(TypeToken<R> responseType);
        
        /// Create DELETE function with three path parameters, no response body expected
        Fn3<Promise<Result<HttpResponse<Unit>>>, T1, T2, T3> delete();
    }
}