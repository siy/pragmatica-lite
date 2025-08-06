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

package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Functions.*;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Implementation of HttpFunctionBuilder0 for functions with no path variables
public record HttpFunctionBuilder0Impl(HttpClient client, String baseUrl, List<String> pathSegments, 
                                      UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder0 {
    
    public HttpFunctionBuilder0Impl {
        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        Objects.requireNonNull(urlBuilder, "URL builder cannot be null");
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 path(String pathSegments) {
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder0Impl(client, baseUrl, newSegments, urlBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(Class<T1> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type), urlBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type.rawType()), urlBuilder);
    }
    
    @Override
    public <R> Fn0<Promise<Result<HttpResponse<R>>>> get(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).get(responseType);
        };
    }
    
    @Override
    public <R> Fn0<Promise<Result<HttpResponse<R>>>> get(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).get(responseType);
        };
    }
    
    @Override
    public Fn0<Promise<Result<HttpResponse<Unit>>>> get() {
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).get();
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> post(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).post(body, responseType);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> post(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).post(body, responseType);
        };
    }
    
    @Override
    public Fn1<Promise<Result<HttpResponse<Unit>>>, Object> post() {
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).post(body);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> put(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).put(body, responseType);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> put(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).put(body, responseType);
        };
    }
    
    @Override
    public Fn1<Promise<Result<HttpResponse<Unit>>>, Object> put() {
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).put(body);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> patch(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).patch(body, responseType);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, Object> patch(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).patch(body, responseType);
        };
    }
    
    @Override
    public Fn1<Promise<Result<HttpResponse<Unit>>>, Object> patch() {
        return body -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).patch(body);
        };
    }
    
    @Override
    public <R> Fn0<Promise<Result<HttpResponse<R>>>> delete(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).delete(responseType);
        };
    }
    
    @Override
    public <R> Fn0<Promise<Result<HttpResponse<R>>>> delete(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).delete(responseType);
        };
    }
    
    @Override
    public Fn0<Promise<Result<HttpResponse<Unit>>>> delete() {
        return () -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, java.util.Map.of());
            return client.request().url(url).delete();
        };
    }
}