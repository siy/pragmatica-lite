package org.pragmatica.net.http.impl;

import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpResponse;
import org.pragmatica.net.http.HttpStatusCode;

public record HttpResponseImpl<T>(
    HttpStatusCode status,
    HttpHeaders headers,
    T body
) implements HttpResponse<T> {
}