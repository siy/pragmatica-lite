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
                                          List<Class<?>> paramTypes, UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder1<T1> {
    
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
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    // === Core Implementation Methods ===
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> json() {
        return new HttpMethodFunctionBuilder1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, CommonContentTypes.APPLICATION_JSON.headerText());
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> json(String contentType) {
        return new HttpMethodFunctionBuilder1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> plainText() {
        return new HttpMethodFunctionBuilder1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, CommonContentTypes.TEXT_PLAIN.headerText());
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> plainText(String contentType) {
        return new HttpMethodFunctionBuilder1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> contentType(String contentType) {
        return new HttpMethodFunctionBuilder1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder1<T1> contentType(ContentType contentType) {
        return contentType(contentType.headerText());
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