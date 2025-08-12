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

/// Implementation of HttpFunctionBuilder0 for functions with no path variables
public record HttpFunctionBuilder0Impl(HttpClient client, String baseUrl, List<String> pathSegments, 
                                      UrlBuilder urlBuilder, HttpClientConfig.Builder configBuilder) implements HttpFunction.HttpFunctionBuilder0 {
    
    public HttpFunctionBuilder0Impl {
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
        return new HttpFunctionBuilder0Impl(client, baseUrl, newSegments, urlBuilder, configBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(Class<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type), urlBuilder, configBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type.rawType()), urlBuilder, configBuilder);
    }
    
    // === Client Configuration Methods ===
    
    private HttpClientConfig.Builder copyBuilder() {
        if (configBuilder == null) {
            return HttpClientConfig.builder();
        }
        var config = configBuilder.build();
        return HttpClientConfig.builder()
            .connectTimeout(config.connectTimeout())
            .requestTimeout(config.requestTimeout())
            .readTimeout(config.readTimeout())
            .maxConnections(config.maxConnections())
            .maxConnectionsPerHost(config.maxConnectionsPerHost())
            .followRedirects(config.followRedirects())
            .userAgent(config.userAgent())
            .defaultHeaders(config.defaultHeaders());
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 connectTimeout(org.pragmatica.lang.io.TimeSpan timeout) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().connectTimeout(timeout));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 requestTimeout(org.pragmatica.lang.io.TimeSpan timeout) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().requestTimeout(timeout));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 readTimeout(org.pragmatica.lang.io.TimeSpan timeout) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().readTimeout(timeout));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 userAgent(String userAgent) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().userAgent(userAgent));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 header(String name, String value) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().defaultHeader(name, value));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 headers(HttpHeaders headers) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().defaultHeaders(headers));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 maxConnections(int maxConnections) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().maxConnections(maxConnections));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 maxConnectionsPerHost(int maxConnectionsPerHost) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().maxConnectionsPerHost(maxConnectionsPerHost));
    }
    
    @Override
    public HttpFunction.HttpFunctionBuilder0 followRedirects(boolean followRedirects) {
        return new HttpFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, copyBuilder().followRedirects(followRedirects));
    }
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodBuilder0 send(ContentType requestContentType) {
        var customClient = configBuilder != null ? HttpClient.create(configBuilder.build()) : client;
        return new HttpMethodBuilder0Impl(customClient, baseUrl, pathSegments, urlBuilder, requestContentType.headerText());
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, Class<T> bodyType) {
        var customClient = configBuilder != null ? HttpClient.create(configBuilder.build()) : client;
        return new HttpMethodBuilderWithBody0Impl<>(customClient, baseUrl, pathSegments, urlBuilder, requestContentType.headerText(), bodyType);
    }
    
    @Override
    public <T> HttpFunction.HttpMethodBuilderWithBody0<T> send(ContentType requestContentType, TypeToken<T> bodyType) {
        var customClient = configBuilder != null ? HttpClient.create(configBuilder.build()) : client;
        return new HttpMethodBuilderWithBody0Impl<>(customClient, baseUrl, pathSegments, urlBuilder, requestContentType.headerText(), bodyType);
    }
}