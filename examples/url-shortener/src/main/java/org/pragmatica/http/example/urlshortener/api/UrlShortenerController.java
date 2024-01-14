package org.pragmatica.http.example.urlshortener.api;

import org.pragmatica.http.HttpError;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.lang.Promise;

/**
 * API Controller.
 * <p>
 * Contains gluing logic which connects external world (HTTP) with internal domain logic.
 */
public interface UrlShortenerController {
    UrlShortenerService service();

    default Promise<UrlShortenerResponse> shortenUrl(UrlShortenerRequest request) {
        return Promise.successful(request.toDomainRequest())                // transform to domain request
                      .flatMap(service()::shortenUrl)                       // Call business logic
                      .map(UrlShortenerResponse::fromShortenedUrl)          // Transform response to API response
                      .mapError(HttpError::unprocessableEntity);            // Map domain errors to HTTP errors
    }
}