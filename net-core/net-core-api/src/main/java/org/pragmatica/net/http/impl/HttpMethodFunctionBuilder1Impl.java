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
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Implementation of HttpMethodFunctionBuilder1 for functions with one path variable
public record HttpMethodFunctionBuilder1Impl<T1>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                 List<Class<?>> paramTypes, UrlBuilder urlBuilder, String contentType) 
    implements HttpFunction.HttpMethodFunctionBuilder1<T1> {
    
    public HttpMethodFunctionBuilder1Impl {
        if (paramTypes.size() != 1) {
            throw new IllegalArgumentException("Expected exactly 1 parameter type, got " + paramTypes.size());
        }
    }
    
    
    @Override
    public <R, U> Fn2<Promise<R>, T1, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType) {
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            var request = client.request()
                .url(url)
                .method(method)
                .header("Content-Type", contentType);
            
            // Only set body if it's not Unit (for methods that don't have a body)
            if (bodyType != Unit.class && body != null) {
                request = request.body(body);
            }
            
            return request.responseType(responseType)
                .send()
                .map(HttpResponse::body)
                .flatMap(optionBody -> optionBody.async());
        };
    }
    
    @Override
    public <R, U> Fn2<Promise<R>, T1, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType) {
        return (param1, body) -> {
            validateParameter(param1, 0);
            var url = buildUrlWithParams(List.of(param1));
            var request = client.request()
                .url(url)
                .method(method)
                .header("Content-Type", contentType);
            
            // Only set body if it's not Unit (for methods that don't have a body)
            if (!bodyType.rawType().equals(Unit.class) && body != null) {
                request = request.body(body);
            }
            
            return request.responseType(responseType)
                .send()
                .map(HttpResponse::body)
                .flatMap(optionBody -> optionBody.async());
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