package org.pragmatica.net.http.impl;

import org.pragmatica.net.http.HttpHeaders;
import org.pragmatica.net.http.HttpResponse;
import org.pragmatica.net.http.HttpStatus;
import org.pragmatica.lang.Result;

public record HttpResponseImpl<T>(
    HttpStatus status,
    HttpHeaders headers,
    Result<T> result
) implements HttpResponse<T> {
}