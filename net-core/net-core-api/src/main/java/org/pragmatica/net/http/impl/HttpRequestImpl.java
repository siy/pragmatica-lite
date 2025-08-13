package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.HttpClient;
import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpMethod;
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpResponse;

public record HttpRequestImpl<T, R>(
    String url,
    HttpMethod method,
    HttpHeaders headers,
    T body,
    TypeToken<R> expectedType,
    HttpClient client
) implements HttpRequest<T, R> {
    
    public HttpRequestImpl {
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType cannot be null");
        }
    }
    
    @Override
    public Promise<HttpResponse<R>> send() {
        return client.exchange(this);
    }
}