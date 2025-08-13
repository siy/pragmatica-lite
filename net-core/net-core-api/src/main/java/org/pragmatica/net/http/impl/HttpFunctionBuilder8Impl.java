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

import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.ArrayList;
import java.util.List;

/// Implementation of HttpFunctionBuilder8 for functions with eight path variables
public record HttpFunctionBuilder8Impl<T1, T2, T3, T4, T5, T6, T7, T8>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                                       List<Class<?>> paramTypes, UrlBuilder urlBuilder) 
    implements HttpFunction.HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> {
    
    public HttpFunctionBuilder8Impl {
        if (paramTypes.size() != 8) {
            throw new IllegalArgumentException("Expected exactly 8 parameter types, got " + paramTypes.size());
        }
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> path(String pathSegments) {
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder8Impl<>(client, baseUrl, newSegments, paramTypes, urlBuilder);
    }
    
    @Override
    public <T9> HttpFunction.HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> pathVar(Class<T9> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type);
        return new HttpFunctionBuilder9Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    @Override
    public <T9> HttpFunction.HttpFunctionBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9> pathVar(TypeToken<T9> type) {
        var newParamTypes = new ArrayList<>(paramTypes);
        newParamTypes.add(type.rawType());
        return new HttpFunctionBuilder9Impl<>(client, baseUrl, pathSegments, newParamTypes, urlBuilder);
    }
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> send(ContentType requestContentType) {
        return new HttpMethodBuilder8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, T> send(ContentType requestContentType, Class<T> bodyType) {
        return new HttpMethodBuilderWithBody8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, bodyType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody8<T1, T2, T3, T4, T5, T6, T7, T8, T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        return new HttpMethodBuilderWithBody8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, bodyType);
    }
}