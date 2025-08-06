package org.pragmatica.net.http;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

import java.net.URI;

public interface HttpRequest<T> {
    
    String url();
    
    HttpMethod method();
    
    HttpHeaders headers();
    
    Object body();
    
    Class<T> responseType();
    
    TypeToken<T> responseTypeToken();
    
    Promise<HttpResponse<T>> send();
    
    static HttpRequestBuilder builder() {
        return HttpRequestBuilder.create();
    }
}