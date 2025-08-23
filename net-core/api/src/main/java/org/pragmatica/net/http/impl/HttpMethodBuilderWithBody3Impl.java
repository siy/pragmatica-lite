package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;
import java.util.List;

/// Stub implementation for HttpMethodBuilderWithBody3
public record HttpMethodBuilderWithBody3Impl<T1, T2, T3, B>(
    HttpClient client, String baseUrl, List<String> pathSegments, UrlBuilder urlBuilder, 
    HttpClientConfig config, ContentType requestContentType, 
    TypeToken<T1> pathVar1Type, TypeToken<T2> pathVar2Type, TypeToken<T3> pathVar3Type, TypeToken<B> bodyType
) implements HttpFunction.HttpMethodBuilderWithBody3<T1, T2, T3, B> {
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> post(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> post(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> post() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> put(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> put(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> put() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> patch(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, R> patch(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilderWithBody3<T1, T2, T3, B, Unit> patch() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
}