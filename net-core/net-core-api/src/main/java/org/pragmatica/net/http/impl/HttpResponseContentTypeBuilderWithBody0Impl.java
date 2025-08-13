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

import java.util.List;
import java.util.Map;

/// Implementation of HttpResponseContentTypeBuilderWithBody0 for no path variables with body
public record HttpResponseContentTypeBuilderWithBody0Impl<B, R>(HttpClient client, String baseUrl, List<String> pathSegments,
                                                               UrlBuilder urlBuilder, String requestContentType, 
                                                               HttpMethod httpMethod, Object bodyTypeInfo, Object responseTypeInfo)
    implements HttpFunction.HttpResponseContentTypeBuilderWithBody0<B, R> {

    @Override
    public Fn1<Promise<R>, B> as(ContentType responseContentType) {
        return (body) -> {
            var url = urlBuilder.buildUrl(baseUrl, pathSegments, Map.of());
            var request = client.request()
                .url(url)
                .method(httpMethod)
                .header("Content-Type", requestContentType)
                .header("Accept", responseContentType.headerText())
                .body(body);

            return switch (responseTypeInfo) {
                case Class<?> responseClass -> request.responseType((Class<R>) responseClass)
                    .send()
                    .map(HttpResponse::body)
                    .flatMap(optionBody -> optionBody.async());
                case TypeToken<?> responseToken -> request.responseType((TypeToken<R>) responseToken)
                    .send()
                    .map(HttpResponse::body)
                    .flatMap(optionBody -> optionBody.async());
                case null, default -> throw new IllegalStateException("Invalid response type info: " + responseTypeInfo);
            };
        };
    }
}