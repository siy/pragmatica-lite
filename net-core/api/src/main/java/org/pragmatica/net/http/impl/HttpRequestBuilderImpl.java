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

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

/// Implementation of HttpRequestBuilder providing fluent API for building HTTP requests.
public class HttpRequestBuilderImpl implements HttpRequestBuilder {
    
    private HttpClient client;
    private String url;
    private HttpMethod method = HttpMethod.GET;
    private HttpHeaders headers = new HttpHeaders();
    private Object body;
    private TypeToken<?> responseType;
    
    public HttpRequestBuilderImpl() {
    }
    
    public HttpRequestBuilderImpl(HttpClient client) {
        this.client = client;
    }
    
    @Override
    public HttpRequestBuilder url(String url) {
        this.url = url;
        return this;
    }
    
    @Override
    public HttpRequestBuilder method(HttpMethod method) {
        this.method = method != null ? method : HttpMethod.GET;
        return this;
    }
    
    @Override
    public HttpRequestBuilder header(String name, String value) {
        if (name != null && value != null) {
            headers.set(name, value);
        }
        return this;
    }
    
    @Override
    public HttpRequestBuilder addHeader(String name, String value) {
        if (name != null && value != null) {
            headers.add(name, value);
        }
        return this;
    }
    
    @Override
    public HttpRequestBuilder headers(HttpHeaders headers) {
        this.headers = headers != null ? new HttpHeaders(headers) : new HttpHeaders();
        return this;
    }
    
    @Override
    public HttpRequestBuilder body(Object body) {
        this.body = body;
        return this;
    }
    
    @Override
    public <R> HttpRequestBuilder responseType(Class<R> responseType) {
        this.responseType = responseType != null ? TypeToken.of(responseType) : null;
        return this;
    }
    
    @Override
    public <R> HttpRequestBuilder responseType(TypeToken<R> responseType) {
        this.responseType = responseType;
        return this;
    }
    
    // === Convenience methods ===
    
    @Override
    public <R> Promise<HttpResponse<R>> get(Class<R> responseType) {
        return method(HttpMethod.GET).responseType(responseType).send();
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> get(TypeToken<R> responseType) {
        return method(HttpMethod.GET).responseType(responseType).send();
    }
    
    @Override
    public Promise<HttpResponse<Unit>> get() {
        return get(Unit.class);
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> post(Object body, Class<R> responseType) {
        return method(HttpMethod.POST).body(body).responseType(responseType).send();
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> post(Object body, TypeToken<R> responseType) {
        return method(HttpMethod.POST).body(body).responseType(responseType).send();
    }
    
    @Override
    public Promise<HttpResponse<Unit>> post(Object body) {
        return post(body, Unit.class);
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> put(Object body, Class<R> responseType) {
        return method(HttpMethod.PUT).body(body).responseType(responseType).send();
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> put(Object body, TypeToken<R> responseType) {
        return method(HttpMethod.PUT).body(body).responseType(responseType).send();
    }
    
    @Override
    public Promise<HttpResponse<Unit>> put(Object body) {
        return put(body, Unit.class);
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> patch(Object body, Class<R> responseType) {
        return method(HttpMethod.PATCH).body(body).responseType(responseType).send();
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> patch(Object body, TypeToken<R> responseType) {
        return method(HttpMethod.PATCH).body(body).responseType(responseType).send();
    }
    
    @Override
    public Promise<HttpResponse<Unit>> patch(Object body) {
        return patch(body, Unit.class);
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> delete(Class<R> responseType) {
        return method(HttpMethod.DELETE).responseType(responseType).send();
    }
    
    @Override
    public <R> Promise<HttpResponse<R>> delete(TypeToken<R> responseType) {
        return method(HttpMethod.DELETE).responseType(responseType).send();
    }
    
    @Override
    public Promise<HttpResponse<Unit>> delete() {
        return delete(Unit.class);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T, R> Promise<HttpResponse<R>> send() {
        if (client == null) {
            return Promise.failure(HttpError.ConfigurationError.create("No HttpClient configured for request"));
        }
        
        if (url == null || url.isBlank()) {
            return Promise.failure(HttpError.ConfigurationError.create("URL is required"));
        }
        
        if (responseType == null) {
            responseType = TypeToken.of(Unit.class);
        }
        
        var request = new HttpRequestImpl<>((T) body, (TypeToken<R>) responseType, url, method, headers, client);
        return client.exchange(request);
    }
}