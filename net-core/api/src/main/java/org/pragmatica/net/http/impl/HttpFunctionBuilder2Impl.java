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

/// Implementation of HttpFunctionBuilder2 for building HTTP functions with two path variables.
public record HttpFunctionBuilder2Impl<T1, T2>(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    TypeToken<T1> pathVar1Type,
    TypeToken<T2> pathVar2Type
) implements HttpFunction.HttpFunctionBuilder2<T1, T2> {
    
    public HttpFunctionBuilder2Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder2<T1, T2> path(String pathSegments) {
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, newSegments, urlBuilder, config, pathVar1Type, pathVar2Type);
    }
    
    @Override
    public <T3> HttpFunction.HttpFunctionBuilder3<T1, T2, T3> pathVar(Class<T3> type) {
        return new HttpFunctionBuilder3Impl<>(client, baseUrl, pathSegments, urlBuilder, config, pathVar1Type, pathVar2Type, TypeToken.of(type));
    }
    
    @Override
    public <T3> HttpFunction.HttpFunctionBuilder3<T1, T2, T3> pathVar(TypeToken<T3> type) {
        return new HttpFunctionBuilder3Impl<>(client, baseUrl, pathSegments, urlBuilder, config, pathVar1Type, pathVar2Type, type);
    }
    
    @Override
    public HttpFunction.HttpMethodBuilder2<T1, T2> send(ContentType requestContentType) {
        return new HttpMethodBuilder2Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type, pathVar2Type);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody2<T1, T2, T> send(ContentType requestContentType, Class<T> bodyType) {
        return new HttpMethodBuilderWithBody2Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type, pathVar2Type, TypeToken.of(bodyType));
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody2<T1, T2, T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        return new HttpMethodBuilderWithBody2Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type, pathVar2Type, bodyType);
    }
}