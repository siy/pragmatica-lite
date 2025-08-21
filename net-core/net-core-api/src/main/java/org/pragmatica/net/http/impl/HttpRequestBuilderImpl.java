package org.pragmatica.net.http.impl;

import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.http.ContentType;
import org.pragmatica.net.http.HttpClient;
import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpMethod;
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpRequestBuilder;

public final class HttpRequestBuilderImpl implements HttpRequestBuilder {
    private String url;
    private HttpMethod method = HttpMethod.GET;
    private HttpHeaders headers = new HttpHeaders();
    private Object body;
    private TimeSpan timeout;
    private HttpClient client;
    private TypeToken<?> responseType;
    
    public HttpRequestBuilderImpl() {
        // Default constructor
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
        this.method = method;
        return this;
    }
    
    @Override
    public HttpRequestBuilder header(String name, String value) {
        this.headers.add(name, value);
        return this;
    }
    
    @Override
    public HttpRequestBuilder headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }
    
    @Override
    public HttpRequestBuilder body(Object body) {
        this.body = body;
        return this;
    }
    
    @Override
    public HttpRequestBuilder timeout(TimeSpan timeout) {
        this.timeout = timeout;
        return this;
    }
    
    
    @Override
    public HttpRequestBuilder contentType(String contentType) {
        return header("Content-Type", contentType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T, R> HttpRequestBuilder responseType(Class<R> responseType) {
        this.responseType = TypeToken.of(responseType);
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T, R> HttpRequestBuilder responseType(TypeToken<R> responseType) {
        this.responseType = responseType;
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T, R> HttpRequest<T, R> build() {
        if (responseType == null) {
            throw new IllegalStateException("Must specify responseType before calling build()");
        }
        return new HttpRequestImpl<>(url, method, headers, (T) body, (TypeToken<R>) responseType, client);
    }
}