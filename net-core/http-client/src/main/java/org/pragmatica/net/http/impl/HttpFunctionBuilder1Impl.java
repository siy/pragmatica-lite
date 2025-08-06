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

/// Implementation of HttpFunctionBuilder1 for functions with one path variable
public record HttpFunctionBuilder1Impl<T1>(HttpClient client, String baseUrl, List<String> pathSegments,
                                          List<Class<?>> paramTypes, UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder1<T1> {
    
    public HttpFunctionBuilder1Impl {
        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        Objects.requireNonNull(paramTypes, "Parameter types cannot be null");
        Objects.requireNonNull(urlBuilder, "URL builder cannot be null");
        if (paramTypes.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 parameter type, got " + paramTypes.size());
        }
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder1<T1> path(String pathSegments) {
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> get(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).get(responseType);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> get(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).get(responseType);
        };
    }
    
    @Override
    public Fn1<Promise<Result<HttpResponse<Unit>>>, T1> get() {
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).get();
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> post(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).post(body, responseType);
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> post(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).post(body, responseType);
        };
    }
    
    @Override
    public Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> post() {
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).post(body);
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> put(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).put(body, responseType);
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> put(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).put(body, responseType);
        };
    }
    
    @Override
    public Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> put() {
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).put(body);
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> patch(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).patch(body, responseType);
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, Object> patch(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).patch(body, responseType);
        };
    }
    
    @Override
    public Fn2<Promise<Result<HttpResponse<Unit>>>, T1, Object> patch() {
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).patch(body);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> delete(Class<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).delete(responseType);
        };
    }
    
    @Override
    public <R> Fn1<Promise<Result<HttpResponse<R>>>, T1> delete(TypeToken<R> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).delete(responseType);
        };
    }
    
    @Override
    public Fn1<Promise<Result<HttpResponse<Unit>>>, T1> delete() {
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            return client.request().url(url).delete();
        };
    }
    
    private void validateParameter(Object param, int index) {
        Objects.requireNonNull(param, "Parameter " + (index + 1) + " cannot be null");
        var expectedType = paramTypes.get(index);
        if (!expectedType.isInstance(param)) {
            throw new IllegalArgumentException(
                "Parameter " + (index + 1) + " expected type " + expectedType.getName() + 
                ", but got " + param.getClass().getName()
            );
        }
    }
    
    private String buildUrlWithParams(List<Object> params) {
        var segments = new ArrayList<>(pathSegments);
        for (var param : params) {
            segments.add(param.toString());
        }
        return urlBuilder.buildUrl(baseUrl, segments, java.util.Map.of());
    }
}