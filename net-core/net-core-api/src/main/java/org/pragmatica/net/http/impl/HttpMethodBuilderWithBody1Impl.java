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
import org.pragmatica.lang.Unit;

import java.util.List;

/// Implementation of HttpMethodBuilderWithBody1 for HTTP methods with one path variable and body
public record HttpMethodBuilderWithBody1Impl<T1, B>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                   List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType, Object bodyTypeInfo)
    implements HttpFunction.HttpMethodBuilderWithBody1<T1, B> {

    // Constructor for Class<B>
    public HttpMethodBuilderWithBody1Impl(HttpClient client, String baseUrl, List<String> pathSegments,
                                         List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType, Class<B> bodyType) {
        this(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, (Object) bodyType);
    }

    // Constructor for TypeToken<B>
    public HttpMethodBuilderWithBody1Impl(HttpClient client, String baseUrl, List<String> pathSegments,
                                         List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType, TypeToken<B> bodyType) {
        this(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, (Object) bodyType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> post(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.POST, bodyTypeInfo, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> post(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.POST, bodyTypeInfo, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> post() {
        return post(Unit.class);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> put(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.PUT, bodyTypeInfo, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> put(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.PUT, bodyTypeInfo, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> put() {
        return put(Unit.class);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> patch(Class<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.PATCH, bodyTypeInfo, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, R> patch(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilderWithBody1Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, 
            requestContentType, HttpMethod.PATCH, bodyTypeInfo, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody1<T1, B, Unit> patch() {
        return patch(Unit.class);
    }
}