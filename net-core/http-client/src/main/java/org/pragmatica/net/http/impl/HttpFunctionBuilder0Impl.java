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
                                      UrlBuilder urlBuilder) implements HttpFunction.HttpFunctionBuilder0 {
    
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
        return new HttpFunctionBuilder0Impl(client, baseUrl, newSegments, urlBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(Class<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type), urlBuilder);
    }
    
    @Override
    public <T1> HttpFunction.HttpFunctionBuilder1<T1> pathVar(TypeToken<T1> type) {
        return new HttpFunctionBuilder1Impl<>(client, baseUrl, pathSegments, List.of(type.rawType()), urlBuilder);
    }
    
    // === Content Type Bridge Methods ===
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 json() {
        return new HttpMethodFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, CommonContentTypes.APPLICATION_JSON.headerText());
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 json(String contentType) {
        return new HttpMethodFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 plainText() {
        return new HttpMethodFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, CommonContentTypes.TEXT_PLAIN.headerText());
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 plainText(String contentType) {
        return new HttpMethodFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 contentType(String contentType) {
        return new HttpMethodFunctionBuilder0Impl(client, baseUrl, pathSegments, urlBuilder, contentType);
    }
    
    @Override
    public HttpFunction.HttpMethodFunctionBuilder0 contentType(ContentType contentType) {
        return contentType(contentType.headerText());
    }
}