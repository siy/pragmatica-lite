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

/// Implementation of HttpFunctionBuilder1 for building HTTP functions with one path variable.
public record HttpFunctionBuilder1Impl<T1>(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    TypeToken<T1> pathVar1Type
) implements HttpFunction.HttpFunctionBuilder1<T1> {
    
    public HttpFunctionBuilder1Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
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
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, newSegments, urlBuilder, config, pathVar1Type);
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(Class<T2> type) {
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, urlBuilder, config, pathVar1Type, TypeToken.of(type));
    }
    
    @Override
    public <T2> HttpFunction.HttpFunctionBuilder2<T1, T2> pathVar(TypeToken<T2> type) {
        return new HttpFunctionBuilder2Impl<>(client, baseUrl, pathSegments, urlBuilder, config, pathVar1Type, type);
    }
    
    @Override
    public HttpFunction.HttpMethodBuilder1<T1> send(ContentType requestContentType) {
        return new HttpMethodBuilder1Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, Class<T> bodyType) {
        return new HttpMethodBuilderWithBody1Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type, TypeToken.of(bodyType));
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody1<T1, T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        return new HttpMethodBuilderWithBody1Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, pathVar1Type, bodyType);
    }
}