package org.pragmatica.http.example;

import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.lang.Result.Cause;

public sealed interface UrlShortenerError extends Cause {
    record MissingInputUrl(String message) implements UrlShortenerError {}

    record InputUrlIsTooLong(String message) implements UrlShortenerError {}

    record OnlyHttpSupported(String message) implements UrlShortenerError {}
    static HttpError missingInputUrl() {
        return HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, new MissingInputUrl("Missing input URL"));
    }

    static HttpError inputUrlIsTooLong() {
        return HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, new InputUrlIsTooLong("Input URL is too long"));
    }

    static HttpError onlyHttpSupported() {
        return HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, new OnlyHttpSupported("Only HTTP/HTTPS URLs are supported"));
    }
}
