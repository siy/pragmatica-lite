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
    public HttpRequestBuilder urlTemplate(String urlTemplate, Object... params) {
        var urlBuilder = UrlBuilder.create();
        this.url = urlBuilder.resolveTemplate(urlTemplate, params);
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
    public HttpRequestBuilder json(String contentType) {
        return header("Content-Type", contentType);
    }
    
    @Override
    public HttpRequestBuilder plainText(String contentType) {
        return header("Content-Type", contentType);
    }
    
    @Override
    public HttpRequestBuilder contentType(String contentType) {
        return header("Content-Type", contentType);
    }
    
    @Override
    public <T> HttpRequest<T> responseType(Class<T> responseType) {
        return new HttpRequestImpl<>(url, method, headers, body, responseType, null, client);
    }
    
    @Override
    public <T> HttpRequest<T> responseType(TypeToken<T> responseType) {
        return new HttpRequestImpl<>(url, method, headers, body, null, responseType, client);
    }
}