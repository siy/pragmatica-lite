package org.pragmatica.net.http.impl;

import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpResponse;

public record HttpResponseImpl<T>(
    int statusCode,
    String statusText,
    HttpHeaders headers,
    T body
) implements HttpResponse<T> {
}