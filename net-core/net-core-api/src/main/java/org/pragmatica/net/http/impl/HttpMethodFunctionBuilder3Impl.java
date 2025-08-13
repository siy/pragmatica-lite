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

/// Implementation of HttpMethodFunctionBuilder3 for functions with three path variables
public record HttpMethodFunctionBuilder3Impl<T1, T2, T3>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                         List<Class<?>> paramTypes, UrlBuilder urlBuilder, String contentType)
    implements HttpFunction.HttpMethodFunctionBuilder3<T1, T2, T3> {
    
    public HttpMethodFunctionBuilder3Impl {
        if (paramTypes.size() != 3) {
            throw new IllegalArgumentException("Expected exactly 3 parameter types, got " + paramTypes.size());
        }
    }
    
    
    @Override
    public <R, U> Fn4<Promise<R>, T1, T2, T3, U> methodWithBody(HttpMethod method, Class<R> responseType, Class<U> bodyType) {
        return (param1, param2, param3, body) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            validateParameter(param3, 2);
            var url = buildUrlWithParams(List.of(param1, param2, param3));
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
    public <R, U> Fn4<Promise<R>, T1, T2, T3, U> methodWithBody(HttpMethod method, TypeToken<R> responseType, TypeToken<U> bodyType) {
        return (param1, param2, param3, body) -> {
            validateParameter(param1, 0);
            validateParameter(param2, 1);
            validateParameter(param3, 2);
            var url = buildUrlWithParams(List.of(param1, param2, param3));
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