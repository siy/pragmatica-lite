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

/// Stub implementation - complete implementation would follow same pattern as HttpMethodBuilder0Impl
public record HttpMethodBuilder1Impl<T1>(
    HttpClient client, String baseUrl, List<String> pathSegments, UrlBuilder urlBuilder, 
    HttpClientConfig config, ContentType requestContentType, TypeToken<T1> pathVar1Type
) implements HttpFunction.HttpMethodBuilder1<T1> {
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder1<T1, R> get(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder1<T1, R> get(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder1<T1, Unit> get() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder1<T1, R> delete(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder1<T1, R> delete(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder1<T1, Unit> delete() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
}