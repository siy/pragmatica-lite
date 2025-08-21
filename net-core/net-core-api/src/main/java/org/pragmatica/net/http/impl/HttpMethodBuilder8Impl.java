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

/// Implementation of HttpMethodBuilder8 for HTTP methods with eight path variables and no body
public record HttpMethodBuilder8Impl<T1, T2, T3, T4, T5, T6, T7, T8>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                                     List<Class<?>> paramTypes, UrlBuilder urlBuilder, ContentType requestContentType)
    implements HttpFunction.HttpMethodBuilder8<T1, T2, T3, T4, T5, T6, T7, T8> {

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> get(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), HttpMethod.GET, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> get(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), HttpMethod.GET, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, Unit> get() {
        return get(Unit.class);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> delete(Class<R> responseType) {
        return new HttpResponseContentTypeBuilder8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), HttpMethod.DELETE, responseType);
    }

    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, R> delete(TypeToken<R> responseType) {
        return new HttpResponseContentTypeBuilder8Impl<>(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType.headerText(), HttpMethod.DELETE, responseType);
    }

    @Override
    public HttpFunction.HttpResponseContentTypeBuilder8<T1, T2, T3, T4, T5, T6, T7, T8, Unit> delete() {
        return delete(Unit.class);
    }
}