package org.pragmatica.net.http;

import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.net.http.impl.HttpRequestBuilderImpl;

public interface HttpRequestBuilder {
    
    HttpRequestBuilder url(String url);
    
    HttpRequestBuilder method(HttpMethod method);
    
    HttpRequestBuilder header(String name, String value);
    
    HttpRequestBuilder headers(HttpHeaders headers);
    
    HttpRequestBuilder body(Object body);
    
    HttpRequestBuilder timeout(TimeSpan timeout);
    
    /// Set custom content type
    HttpRequestBuilder contentType(String contentType);
    
    /// Set content type using ContentType interface
    default HttpRequestBuilder contentType(ContentType contentType) {
        return contentType(contentType.headerText());
    }
    
    <T> HttpRequest<T> responseType(Class<T> responseType);
    
    <T> HttpRequest<T> responseType(TypeToken<T> responseType);
    
    <T> HttpRequest<T> send();
    
    
    static HttpRequestBuilder create() {
        return new HttpRequestBuilderImpl();
    }
    
    static HttpRequestBuilder create(HttpClient client) {
        return new HttpRequestBuilderImpl(client);
    }
}