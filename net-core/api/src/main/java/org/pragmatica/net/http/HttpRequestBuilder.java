package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.io.TimeSpan;

public interface HttpRequestBuilder {
    
    HttpRequestBuilder url(String url);
    
    HttpRequestBuilder urlTemplate(String urlTemplate, Object... params);
    
    HttpRequestBuilder method(HttpMethod method);
    
    HttpRequestBuilder header(String name, String value);
    
    HttpRequestBuilder headers(HttpHeaders headers);
    
    HttpRequestBuilder body(Object body);
    
    HttpRequestBuilder timeout(TimeSpan timeout);
    
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
    
    // === Template-style HTTP Methods ===
    
    /// GET with URL template
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, Class<R> responseType) {
        return urlTemplate(urlTemplate, params).get(responseType);
    }
    
    /// GET with URL template and TypeToken
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, TypeToken<R> responseType) {
        return urlTemplate(urlTemplate, params).get(responseType);
    }
    
    /// GET with URL template, no response body expected
    default Promise<HttpResponse<Unit>> get(String urlTemplate, Object[] params) {
        return urlTemplate(urlTemplate, params).get(Unit.class);
    }
    
    /// POST with URL template
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, Class<R> responseType) {
        return urlTemplate(urlTemplate, params).post(body, responseType);
    }
    
    /// POST with URL template and TypeToken
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, TypeToken<R> responseType) {
        return urlTemplate(urlTemplate, params).post(body, responseType);
    }
    
    /// POST with URL template, no response body expected
    default Promise<HttpResponse<Unit>> post(String urlTemplate, Object[] params, Object body) {
        return urlTemplate(urlTemplate, params).method(HttpMethod.POST).body(body).responseType(Unit.class).send();
    }
    
    // === Template Methods with Content Type ===
    
    /// GET with URL template and content type
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, String contentType, Class<R> responseType) {
        return urlTemplate(urlTemplate, params).contentType(contentType).get(responseType);
    }
    
    /// GET with URL template, content type and TypeToken
    default <R> Promise<HttpResponse<R>> get(String urlTemplate, Object[] params, String contentType, TypeToken<R> responseType) {
        return urlTemplate(urlTemplate, params).contentType(contentType).get(responseType);
    }
    
    /// POST with URL template and content type
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, String contentType, Class<R> responseType) {
        return urlTemplate(urlTemplate, params).contentType(contentType).post(body, responseType);
    }
    
    /// POST with URL template, content type and TypeToken
    default <R> Promise<HttpResponse<R>> post(String urlTemplate, Object[] params, Object body, String contentType, TypeToken<R> responseType) {
        return urlTemplate(urlTemplate, params).contentType(contentType).post(body, responseType);
    }
    
    static HttpRequestBuilder create() {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl();
    }
    
    static HttpRequestBuilder create(HttpClient client) {
        return new org.pragmatica.net.http.impl.HttpRequestBuilderImpl(client);
    }
}