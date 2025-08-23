package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;
import java.util.List;

/// Stub implementation for HttpMethodBuilder3
public record HttpMethodBuilder3Impl<T1, T2, T3>(
    HttpClient client, String baseUrl, List<String> pathSegments, UrlBuilder urlBuilder, 
    HttpClientConfig config, ContentType requestContentType, 
    TypeToken<T1> pathVar1Type, TypeToken<T2> pathVar2Type, TypeToken<T3> pathVar3Type
) implements HttpFunction.HttpMethodBuilder3<T1, T2, T3> {
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, R> get(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, R> get(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, Unit> get() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, R> delete(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, R> delete(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder3<T1, T2, T3, Unit> delete() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
}