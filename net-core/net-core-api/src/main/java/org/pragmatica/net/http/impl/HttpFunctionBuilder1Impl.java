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

/// Implementation of HttpFunctionBuilder1 for functions with one path variable
public record HttpFunctionBuilder1Impl<T1>(HttpClient client, String baseUrl, List<String> pathSegments,
                                          List<Class<?>> paramTypes, UrlBuilder urlBuilder, HttpClientConfig.Builder configBuilder) implements HttpFunction.HttpFunctionBuilder1<T1> {
    
    public HttpFunctionBuilder1Impl {
        if (paramTypes.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 parameter type, got " + paramTypes.size());
        }
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder1<T1> path(String pathSegments) {
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder, configBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        var customClient = getOrCreateCustomClient();
        return new HttpFunctionBuilder2Impl<>(customClient, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        var customClient = getOrCreateCustomClient();
        return new HttpFunctionBuilder2Impl<>(customClient, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    // === Core Implementation Methods ===
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodBuilder1<T1> send(ContentType requestContentType) {
        var customClient = getOrCreateCustomClient();
        return new HttpMethodBuilder1Impl<>(customClient, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText());
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, Class<T> bodyType) {
        var customClient = getOrCreateCustomClient();
        return new HttpMethodBuilderWithBody1Impl<>(customClient, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), bodyType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        var customClient = getOrCreateCustomClient();
        return new HttpMethodBuilderWithBody1Impl<>(customClient, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), bodyType);
    }
    
    private HttpClient getOrCreateCustomClient() {
        return configBuilder != null ? HttpClient.create(configBuilder.build()) : client;
    }
    
    
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