package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.net.URI;

public interface HttpRequest<T, R> {
    
    String url();
    
    HttpMethod method();
    
    HttpHeaders headers();
    
    T body();
    
    TypeToken<R> expectedType();
    
    Promise<HttpResponse<R>> send();
    
    static HttpRequestBuilder builder() {
        return HttpRequestBuilder.create();
    }
}