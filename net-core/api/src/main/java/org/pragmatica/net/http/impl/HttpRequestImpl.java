package org.pragmatica.net.http.impl;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.net.http.HttpClient;
import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpMethod;
import org.pragmatica.net.http.HttpRequest;
import org.pragmatica.net.http.HttpResponse;

public record HttpRequestImpl<T>(
    String url,
    HttpMethod method,
    HttpHeaders headers,
    Object body,
    Class<T> responseType,
    TypeToken<T> responseTypeToken,
    HttpClient client
) implements HttpRequest<T> {
    
    @Override
    public Promise<HttpResponse<T>> send() {
        return client.send(this);
    }
}