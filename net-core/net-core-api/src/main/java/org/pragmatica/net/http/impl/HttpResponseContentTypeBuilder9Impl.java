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
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Implementation of HttpResponseContentTypeBuilder9 for nine path variables
public record HttpResponseContentTypeBuilder9Impl<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                                                         List<Class<?>> paramTypes, UrlBuilder urlBuilder, 
                                                                                         String requestContentType, HttpMethod httpMethod, 
                                                                                         Object responseTypeInfo)
    implements HttpFunction.HttpResponseContentTypeBuilder9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {

    public HttpResponseContentTypeBuilder9Impl {
        if (paramTypes.size() != 9) {
            throw new IllegalArgumentException("Expected exactly 9 parameter types, got " + paramTypes.size());
        }
    }

    // Constructor for Class<R>
    public HttpResponseContentTypeBuilder9Impl(HttpClient client, String baseUrl, List<String> pathSegments,
                                              List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType,
                                              HttpMethod httpMethod, Class<R> responseType) {
        this(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, httpMethod, (Object) responseType);
    }

    // Constructor for TypeToken<R>
    public HttpResponseContentTypeBuilder9Impl(HttpClient client, String baseUrl, List<String> pathSegments,
                                              List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType,
                                              HttpMethod httpMethod, TypeToken<R> responseType) {
        this(client, baseUrl, pathSegments, paramTypes, urlBuilder, requestContentType, httpMethod, (Object) responseType);
    }

    @Override
    public Fn9<Promise<R>, T1, T2, T3, T4, T5, T6, T7, T8, T9> as(ContentType responseContentType) {
        return (param1, param2, param3, param4, param5, param6, param7, param8, param9) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            validateParameter(param3, 2);
            validateParameter(param4, 3);
            validateParameter(param5, 4);
            validateParameter(param6, 5);
            validateParameter(param7, 6);
            validateParameter(param8, 7);
            validateParameter(param9, 8);
            var url = buildUrlWithParams(List.of(param1, param2, param3, param4, param5, param6, param7, param8, param9));
            var request = client.request()
                .url(url)
                .method(httpMethod)
                .header("Content-Type", requestContentType)
                .header("Accept", responseContentType.headerText());

            return switch (responseTypeInfo) {
                case Class<?> responseClass -> {
                    var httpRequest = request.responseType((Class<R>) responseClass);
                    yield client.exchange(httpRequest)
                        .map(HttpResponse::result)
                        .flatMap(resultBody -> resultBody.async());
                }
                case TypeToken<?> responseToken -> {
                    var httpRequest = request.responseType((TypeToken<R>) responseToken);
                    yield client.exchange(httpRequest)
                        .map(HttpResponse::result)
                        .flatMap(resultBody -> resultBody.async());
                }
                case null, default -> throw new IllegalStateException("Invalid response type info: " + responseTypeInfo);
            };
        };
    }

    private void validateParameter(Object param, int index) {
        var expectedType = paramTypes.get(index);
        if (!expectedType.isInstance(param)) {
            throw new IllegalArgumentException(
                "Parameter " + (index + 1) + " expected type " + expectedType.getName() + 
                ", but got " + param.getClass().getName()
            );
        }
    }

    private String buildUrlWithParams(List<Object> params) {
        var segments = new ArrayList<>(pathSegments);
        for (var param : params) {
            segments.add(param.toString());
        }
        return urlBuilder.buildUrl(baseUrl, segments, Map.of());
    }
}