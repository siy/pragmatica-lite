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
    
    <T, R> HttpRequestBuilder responseType(Class<R> responseType);
    
    <T, R> HttpRequestBuilder responseType(TypeToken<R> responseType);
    
    <T, R> HttpRequest<T, R> build();
    
    
    static HttpRequestBuilder create() {
        return new HttpRequestBuilderImpl();
    }
    
    static HttpRequestBuilder create(HttpClient client) {
        return new HttpRequestBuilderImpl(client);
    }
}