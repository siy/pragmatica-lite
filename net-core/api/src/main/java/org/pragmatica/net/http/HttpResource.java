package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

public interface HttpResource {
    
    String baseUrl();
    
    String path();
    
    HttpHeaders headers();
    
    HttpResource path(String pathSegment);
    
    HttpResource queryParam(String name, String value);
    
    HttpResource header(String name, String value);
    
    HttpResource headers(HttpHeaders headers);
    
    // CRUD operations
    <T> Promise<HttpResponse<T>> get(Class<T> responseType);
    
    <T> Promise<HttpResponse<T>> get(TypeToken<T> responseType);
    
    <T> Promise<HttpResponse<T>> post(Object body, Class<T> responseType);
    
    <T> Promise<HttpResponse<T>> post(Object body, TypeToken<T> responseType);
    
    <T> Promise<HttpResponse<T>> put(Object body, Class<T> responseType);
    
    <T> Promise<HttpResponse<T>> put(Object body, TypeToken<T> responseType);
    
    <T> Promise<HttpResponse<T>> patch(Object body, Class<T> responseType);
    
    <T> Promise<HttpResponse<T>> patch(Object body, TypeToken<T> responseType);
    
    <T> Promise<HttpResponse<T>> delete(Class<T> responseType);
    
    <T> Promise<HttpResponse<T>> delete(TypeToken<T> responseType);
    
    // Builder-style access to request builder
    HttpRequestBuilder request();
    
    static HttpResource create(HttpClient client, String baseUrl) {
        return new org.pragmatica.net.http.impl.HttpResourceImpl(client, baseUrl);
    }
}