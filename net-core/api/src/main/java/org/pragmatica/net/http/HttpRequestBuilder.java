package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.time.Duration;

public interface HttpRequestBuilder {
    
    HttpRequestBuilder url(String url);
    
    HttpRequestBuilder method(HttpMethod method);
    
    HttpRequestBuilder header(String name, String value);
    
    HttpRequestBuilder headers(HttpHeaders headers);
    
    HttpRequestBuilder body(Object body);
    
    HttpRequestBuilder timeout(Duration timeout);
    
    // === Content Type Configuration ===
    
    /// Set JSON content type for request/response
    default HttpRequestBuilder json() {
        return contentType(CommonContentTypes.APPLICATION_JSON);
    }
    
    /// Set JSON with specific content type
    HttpRequestBuilder json(String contentType);
    
    /// Set plain text content type
    default HttpRequestBuilder plainText() {
        return contentType(CommonContentTypes.TEXT_PLAIN);
    }
    
    /// Set plain text with specific content type  
    HttpRequestBuilder plainText(String contentType);
    
    /// Set custom content type
    HttpRequestBuilder contentType(String contentType);
    
    /// Set content type using ContentType interface
    default HttpRequestBuilder contentType(ContentType contentType) {
        return contentType(contentType.headerText());
    }
    
    <T> HttpRequest<T> responseType(Class<T> responseType);
    
    <T> HttpRequest<T> responseType(TypeToken<T> responseType);
    
    // Convenience methods for common HTTP methods
    default <T> Promise<HttpResponse<T>> get(Class<T> responseType) {
        return method(HttpMethod.GET).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> get(TypeToken<T> responseType) {
        return method(HttpMethod.GET).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> post(Object body, Class<T> responseType) {
        return method(HttpMethod.POST).body(body).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> post(Object body, TypeToken<T> responseType) {
        return method(HttpMethod.POST).body(body).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> put(Object body, Class<T> responseType) {
        return method(HttpMethod.PUT).body(body).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> put(Object body, TypeToken<T> responseType) {
        return method(HttpMethod.PUT).body(body).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> delete(Class<T> responseType) {
        return method(HttpMethod.DELETE).responseType(responseType).send();
    }
    
    default <T> Promise<HttpResponse<T>> delete(TypeToken<T> responseType) {
        return method(HttpMethod.DELETE).responseType(responseType).send();
    }
    
    static HttpRequestBuilder create() {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl();
    }
    
    static HttpRequestBuilder create(HttpClient client) {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl(client);
    }
}