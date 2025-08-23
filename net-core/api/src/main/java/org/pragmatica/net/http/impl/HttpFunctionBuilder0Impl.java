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
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.ArrayList;
import java.util.List;

/// Implementation of HttpFunctionBuilder0 for building HTTP functions with no path variables.
public record HttpFunctionBuilder0Impl(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config
) implements HttpFunction.HttpFunctionBuilder0 {
    
    public HttpFunctionBuilder0Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 path(String pathSegments) {
        var segments = pathSegments.split("/");
        var newSegments = new ArrayList<>(this.pathSegments);
        for (var segment : segments) {
            if (!segment.isEmpty()) {
                newSegments.add(segment);
            }
        }
        return new HttpFunctionBuilder0Impl(client, baseUrl, newSegments, urlBuilder, config);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(Class<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, urlBuilder, config, TypeToken.of(type));
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, urlBuilder, config, type);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 connectTimeout(TimeSpan timeout) {
        var newConfig = config != null ? config.toBuilder().connectTimeout(timeout).build() : 
                        HttpClientConfig.defaults().toBuilder().connectTimeout(timeout).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 requestTimeout(TimeSpan timeout) {
        var newConfig = config != null ? config.toBuilder().requestTimeout(timeout).build() : 
                        HttpClientConfig.defaults().toBuilder().requestTimeout(timeout).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 readTimeout(TimeSpan timeout) {
        var newConfig = config != null ? config.toBuilder().readTimeout(timeout).build() : 
                        HttpClientConfig.defaults().toBuilder().readTimeout(timeout).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 userAgent(String userAgent) {
        var newConfig = config != null ? config.toBuilder().userAgent(userAgent).build() : 
                        HttpClientConfig.defaults().toBuilder().userAgent(userAgent).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 header(String name, String value) {
        var newConfig = config != null ? config.toBuilder().addDefaultHeader(name, value).build() : 
                        HttpClientConfig.defaults().toBuilder().addDefaultHeader(name, value).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 headers(HttpHeaders headers) {
        var newConfig = config != null ? config.toBuilder().defaultHeaders(headers).build() : 
                        HttpClientConfig.defaults().toBuilder().defaultHeaders(headers).build();
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, newConfig);
    }
    
    @Override
    public HttpFunction.HttpMethodBuilder0 send(ContentType requestContentType) {
        return new HttpMethodBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, config, requestContentType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, Class<T> bodyType) {
        return new HttpMethodBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, TypeToken.of(bodyType));
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        return new HttpMethodBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, bodyType);
    }
}