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

/// Implementation of HttpMethodBuilder0 for building HTTP methods with no path variables.
public record HttpMethodBuilder0Impl(
    HttpClient client,
    String baseUrl,
    List<String> pathSegments,
    UrlBuilder urlBuilder,
    HttpClientConfig config,
    ContentType requestContentType
) implements HttpFunction.HttpMethodBuilder0 {
    
    public HttpMethodBuilder0Impl {
        // Ensure immutable collections
        pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder0<R> get(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.GET, null, TypeToken.of(responseType));
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder0<R> get(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.GET, null, responseType);
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder0<Unit> get() {
        return get(Unit.class);
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder0<R> delete(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.DELETE, null, TypeToken.of(responseType));
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder0<R> delete(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder0Impl<>(client, baseUrl, pathSegments, urlBuilder, config, requestContentType, HttpMethod.DELETE, null, responseType);
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder0<Unit> delete() {
        return delete(Unit.class);
    }
}