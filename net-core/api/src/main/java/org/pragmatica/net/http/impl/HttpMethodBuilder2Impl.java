package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;
import java.util.List;

/// Stub implementation for HttpMethodBuilder2
public record HttpMethodBuilder2Impl<T1, T2>(
    HttpClient client, String baseUrl, List<String> pathSegments, UrlBuilder urlBuilder, 
    HttpClientConfig config, ContentType requestContentType, TypeToken<T1> pathVar1Type, TypeToken<T2> pathVar2Type
) implements HttpFunction.HttpMethodBuilder2<T1, T2> {
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, R> get(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, R> get(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, Unit> get() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, R> delete(Class<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public <R> HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, R> delete(TypeToken<R> responseType) {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
    
    @Override
    public HttpFunction.HttpResponseContentTypeBuilder2<T1, T2, Unit> delete() {
        throw new UnsupportedOperationException("Not implemented in this stub version");
    }
}