package org.pragmatica.net.http.impl;

import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.HttpClient;
import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpMethod;
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpRequestBuilder;

import java.time.Duration;
import java.util.Objects;

public final class HttpRequestBuilderImpl implements HttpRequestBuilder {
    private String url;
    private HttpMethod method = HttpMethod.GET;
    private HttpHeaders headers = new HttpHeaders();
    private Object body;
    private Duration timeout;
    private HttpClient client;
    
    public HttpRequestBuilderImpl() {
        // Default constructor
    }
    
    public HttpRequestBuilderImpl(HttpClient client) {
        this.client = client;
    }
    
    @Override
    public HttpRequestBuilder url(String url) {
        this.url = Objects.requireNonNull(url, "URL cannot be null");
        return this;
    }
    
    @Override
    public HttpRequestBuilder method(HttpMethod method) {
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        return this;
    }
    
    @Override
    public HttpRequestBuilder header(String name, String value) {
        this.headers.add(name, value);
        return this;
    }
    
    @Override
    public HttpRequestBuilder headers(HttpHeaders headers) {
        this.headers = Objects.requireNonNull(headers, "Headers cannot be null");
        return this;
    }
    
    @Override
    public HttpRequestBuilder body(Object body) {
        this.body = body;
        return this;
    }
    
    @Override
    public HttpRequestBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }
    
    @Override
    public <T> HttpRequest<T> responseType(Class<T> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return new HttpRequestImpl<>(url, method, headers, body, responseType, null, client);
    }
    
    @Override
    public <T> HttpRequest<T> responseType(TypeToken<T> responseType) {
        Objects.requireNonNull(responseType, "Response type cannot be null");
        return new HttpRequestImpl<>(url, method, headers, body, null, responseType, client);
    }
}