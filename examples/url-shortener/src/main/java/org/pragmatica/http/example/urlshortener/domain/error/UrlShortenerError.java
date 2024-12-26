package org.pragmatica.http.example.urlshortener.domain.error;

import org.pragmatica.lang.Cause;

/**
 * Business logic domain level errors. Modelled as sealed interface, but in many cases simple enum is enough.
 */
public sealed interface UrlShortenerError extends Cause {
    record MissingInputUrl(String message) implements UrlShortenerError {}

    record InputUrlIsTooLong(String message) implements UrlShortenerError {}

    record OnlyHttpSupported(String message) implements UrlShortenerError {}
}
