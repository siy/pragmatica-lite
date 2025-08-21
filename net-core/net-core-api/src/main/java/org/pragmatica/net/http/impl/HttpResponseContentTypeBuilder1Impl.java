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

/// Implementation of HttpResponseContentTypeBuilder1 for functions with one path variable
public record HttpResponseContentTypeBuilder1Impl<T1, R>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                        List<Class<?>> paramTypes, UrlBuilder urlBuilder, String requestContentType,
                                                        HttpMethod method, Object responseType) implements HttpFunction.HttpResponseContentTypeBuilder1<T1, R> {

    @Override
    public Fn1<Promise<R>, T1> as(ContentType responseContentType) {
        return param1 -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            var request = client.request()
                .method(method)
                .url(url)
                .header("Accept", responseContentType.headerText())
                .header("Content-Type", requestContentType);
                
            return switch (responseType) {
                case Class<?> responseClass -> {
                    var httpRequest = request.responseType((Class<R>) responseClass).build();
                    yield client.exchange(httpRequest)
                        .map(response -> response.result().unwrap());
                }
                case TypeToken<?> responseToken -> {
                    var httpRequest = request.responseType((TypeToken<R>) responseToken).build();
                    yield client.exchange(httpRequest)
                        .map(response -> response.result().unwrap());
                }
                case null, default -> throw new IllegalStateException("Invalid response type info: " + responseType);
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