package org.pragmatica.net.http.impl;

import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpResponse;
import org.pragmatica.net.http.HttpError;

public record HttpResponseImpl<T>(
    HttpError error,
    HttpHeaders headers,
    T body
) implements HttpResponse<T> {
    
    @Override
    public int statusCode() {
        return error.code().code();
    }
    
    @Override
    public String statusText() {
        return error.text();
    }
}