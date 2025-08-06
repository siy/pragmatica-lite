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

/// Implementation of HttpFunctionBuilder2 for functions with two path variables
public record HttpFunctionBuilder2Impl<T1, T2>(HttpClient client, String baseUrl, List<String> pathSegments,
                                              List<Class<?>> paramTypes, UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder2<T1, T2> {
    
    public HttpFunctionBuilder2Impl {
        Objects.requireNonNull(client, "Client cannot be null");
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        Objects.requireNonNull(paramTypes, "Parameter types cannot be null");
        Objects.requireNonNull(urlBuilder, "URL builder cannot be null");
        if (paramTypes.size() != 2) {
            throw new IllegalArgumentException("Expected exactly 2 parameter types, got " + paramTypes.size());
        }
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder2<T1, T2> path(String pathSegments) {
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder);
    }
    
    @Override
    public <T3> HttpFunction.HttpFunctionBuilder3<T1, T2, T3> pathVar(Class<T3> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        return new HttpFunctionBuilder3Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T3> HttpFunction.HttpFunctionBuilder3<T1, T2, T3> pathVar(TypeToken<T3> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        return new HttpFunctionBuilder3Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    // === Core Implementation Methods ===
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> method(HttpMethod method, Class<R> responseType) {
        Objects.requireNonNull(method, "HTTP method cannot be null");
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, param2) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            var url = buildUrlWithParams(List.of(param1, param2));
            return client.request().url(url).method(method).responseType(responseType).send();
        };
    }
    
    @Override
    public <R> Fn2<Promise<Result<HttpResponse<R>>>, T1, T2> method(HttpMethod method, TypeToken<R> responseType) {
        Objects.requireNonNull(method, "HTTP method cannot be null");
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, param2) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            var url = buildUrlWithParams(List.of(param1, param2));
            return client.request().url(url).method(method).responseType(responseType).send();
        };
    }
    
    @Override
    public <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> methodWithBody(HttpMethod method, Class<R> responseType) {
        Objects.requireNonNull(method, "HTTP method cannot be null");
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, param2, body) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            var url = buildUrlWithParams(List.of(param1, param2));
            return client.request().url(url).method(method).body(body).responseType(responseType).send();
        };
    }
    
    @Override
    public <R> Fn3<Promise<Result<HttpResponse<R>>>, T1, T2, Object> methodWithBody(HttpMethod method, TypeToken<R> responseType) {
        Objects.requireNonNull(method, "HTTP method cannot be null");
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return (param1, param2, body) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            var url = buildUrlWithParams(List.of(param1, param2));
            return client.request().url(url).method(method).body(body).responseType(responseType).send();
        };
    }
    
    // === Helper Methods ===
    
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