package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class HttpResourceImpl implements HttpResource {
    private final HttpClient client;
    private final String baseUrl;
    private final List<String> pathSegments;
    private final List<QueryParam> queryParams;
    private final HttpHeaders headers;
    
    private record QueryParam(String name, String value) {}
    
    public HttpResourceImpl(HttpClient client, String baseUrl) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.pathSegments = new ArrayList<>();
        this.queryParams = new ArrayList<>();
        this.headers = new HttpHeaders();
    }
    
    private HttpResourceImpl(HttpClient client, String baseUrl, List<String> pathSegments, 
                           List<QueryParam> queryParams, HttpHeaders headers) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.pathSegments = new ArrayList<>(pathSegments);
        this.queryParams = new ArrayList<>(queryParams);
        this.headers = new HttpHeaders();
        // Copy headers
        for (var name : headers.names()) {
            for (var value : headers.all(name)) {
                this.headers.add(name, value);
            }
        }
    }
    
    @Override
    public String baseUrl() {
        return baseUrl;
    }
    
    @Override
    public String path() {
        return String.join("/", pathSegments);
    }
    
    @Override
    public HttpHeaders headers() {
        return headers;
    }
    
    @Override
    public HttpResource path(String pathSegment) {
        var newSegments = new ArrayList<>(pathSegments);
        newSegments.add(pathSegment);
        return new HttpResourceImpl(client, baseUrl, newSegments, queryParams, headers);
    }
    
    @Override
    public HttpResource queryParam(String name, String value) {
        var newParams = new ArrayList<>(queryParams);
        newParams.add(new QueryParam(name, value));
        return new HttpResourceImpl(client, baseUrl, pathSegments, newParams, headers);
    }
    
    @Override
    public HttpResource header(String name, String value) {
        var newHeaders = new HttpHeaders();
        // Copy existing headers
        for (var headerName : headers.names()) {
            for (var headerValue : headers.all(headerName)) {
                newHeaders.add(headerName, headerValue);
            }
        }
        newHeaders.add(name, value);
        return new HttpResourceImpl(client, baseUrl, pathSegments, queryParams, newHeaders);
    }
    
    @Override
    public HttpResource headers(HttpHeaders headers) {
        return new HttpResourceImpl(client, baseUrl, pathSegments, queryParams, headers);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> get(Class<T> responseType) {
        return createRequest().get(responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> get(TypeToken<T> responseType) {
        return createRequest().get(responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> post(Object body, Class<T> responseType) {
        return createRequest().post(body, responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> post(Object body, TypeToken<T> responseType) {
        return createRequest().post(body, responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> put(Object body, Class<T> responseType) {
        return createRequest().put(body, responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> put(Object body, TypeToken<T> responseType) {
        return createRequest().put(body, responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> patch(Object body, Class<T> responseType) {
        return createRequest().method(HttpMethod.PATCH).body(body).responseType(responseType).send();
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> patch(Object body, TypeToken<T> responseType) {
        return createRequest().method(HttpMethod.PATCH).body(body).responseType(responseType).send();
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> delete(Class<T> responseType) {
        return createRequest().delete(responseType);
    }
    
    @Override
    public <T> Promise<HttpResponse<T>> delete(TypeToken<T> responseType) {
        return createRequest().delete(responseType);
    }
    
    @Override
    public HttpRequestBuilder request() {
        return createRequest();
    }
    
    private HttpRequestBuilder createRequest() {
        var url = buildUrl();
        var builder = client.request().url(url);
        
        // Add headers
        for (var name : headers.names()) {
            for (var value : headers.all(name)) {
                builder.header(name, value);
            }
        }
        
        return builder;
    }
    
    private String buildUrl() {
        var url = new StringBuilder(baseUrl);
        
        // Add path segments
        for (var segment : pathSegments) {
            if (!url.toString().endsWith("/")) {
                url.append("/");
            }
            url.append(URLEncoder.encode(segment, StandardCharsets.UTF_8));
        }
        
        // Add query parameters
        if (!queryParams.isEmpty()) {
            url.append("?");
            for (int i = 0; i < queryParams.size(); i++) {
                if (i > 0) {
                    url.append("&");
                }
                var param = queryParams.get(i);
                url.append(URLEncoder.encode(param.name(), StandardCharsets.UTF_8))
                   .append("=")
                   .append(URLEncoder.encode(param.value(), StandardCharsets.UTF_8));
            }
        }
        
        return url.toString();
    }
}