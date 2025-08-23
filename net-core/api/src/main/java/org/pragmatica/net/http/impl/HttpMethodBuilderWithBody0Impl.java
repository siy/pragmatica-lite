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

import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.List;

/// Implementation of HttpMethodBuilderWithBody0 for building HTTP methods with body and no path variables.
public record HttpMethodBuilderWithBody0Impl<B>(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    ContentType requestContentType,
    TypeToken<B> bodyType
) implements HttpFunction.HttpMethodBuilderWithBody0<B> {
    
    public HttpMethodBuilderWithBody0Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> post(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.POST, bodyType, TypeToken.of(responseType));
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> post(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.POST, bodyType, responseType);
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, Unit> post() {
        return post(Unit.class);
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> put(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.PUT, bodyType, TypeToken.of(responseType));
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> put(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.PUT, bodyType, responseType);
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, Unit> put() {
        return put(Unit.class);
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> patch(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.PATCH, bodyType, TypeToken.of(responseType));
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> patch(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.PATCH, bodyType, responseType);
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, Unit> patch() {
        return patch(Unit.class);
    }
}