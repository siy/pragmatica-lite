package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;
import java.util.List;

/// Stub implementation for HttpMethodBuilderWithBody2
public record HttpMethodBuilderWithBody2Impl<T1, T2, B>(
    HttpClient client, String baseUrl, List<String> pathSegments, UrlBuilder urlBuilder, 
    HttpClientConfig config, ContentType requestContentType, 
    TypeToken<T1> pathVar1Type, TypeToken<T2> pathVar2Type, TypeToken<B> bodyType
) implements HttpFunction.HttpMethodBuilderWithBody2<T1, T2, B> {
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> post(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> post(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> post() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> put(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> put(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> put() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> patch(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, R> patch(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody2<T1, T2, B, Unit> patch() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
}