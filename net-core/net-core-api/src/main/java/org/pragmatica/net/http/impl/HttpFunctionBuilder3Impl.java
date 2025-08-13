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
import java.util.Map;

/// Implementation of HttpFunctionBuilder3 for functions with three path variables
public record HttpFunctionBuilder3Impl<T1, T2, T3>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                  List<Class<?>> paramTypes, UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder3<T1, T2, T3> {
    
    public HttpFunctionBuilder3Impl {
        if (paramTypes.size() != 3) {
            throw new IllegalArgumentException("Expected exactly 3 parameter types, got " + paramTypes.size());
        }
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder3<T1, T2, T3> path(String pathSegments) {
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder3Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder);
    }
    
    @Override
    public <T4> HttpFunction.HttpFunctionBuilder4<T1, T2, T3, T4> pathVar(Class<T4> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        return new HttpFunctionBuilder4Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T4> HttpFunction.HttpFunctionBuilder4<T1, T2, T3, T4> pathVar(TypeToken<T4> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        return new HttpFunctionBuilder4Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodBuilder3<T1, T2, T3> send(ContentType requestContentType) {
        return new HttpMethodBuilder3Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody3<T1, T2, T3, T> send(ContentType requestContentType, Class<T> bodyType) {
        return new HttpMethodBuilderWithBody3Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, bodyType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody3<T1, T2, T3, T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        return new HttpMethodBuilderWithBody3Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, bodyType);
    }
    
    // === Helper Methods ===
    
    private void validateParameter(Object param, int index) {
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
        return urlBuilder.buildUrl(baseUrl, segments, Map.of());
    }
}