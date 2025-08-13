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

/// Implementation of HttpMethodBuilder5 for HTTP methods with five path variables and no body
public record HttpMethodBuilder5Impl<T1, T2, T3, T4, T5>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                         List<Class<?>> paramTypes, UrlBuilder urlBuilder, ContentType requestContentType)
    implements HttpFunction.HttpMethodBuilder5<T1, T2, T3, T4, T5> {

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> get(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder5Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, HttpMethod.GET, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> get(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder5Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, HttpMethod.GET, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, Unit> get() {
        return get(Unit.class);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> delete(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder5Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, HttpMethod.DELETE, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, R> delete(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder5Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, HttpMethod.DELETE, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilder5<T1, T2, T3, T4, T5, Unit> delete() {
        return delete(Unit.class);
    }
}