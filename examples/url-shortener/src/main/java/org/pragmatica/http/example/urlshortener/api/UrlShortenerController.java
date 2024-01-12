package org.pragmatica.http.example.urlshortener.api;

import org.pragmatica.http.HttpError;
import org.pragmatica.http.example.urlshortener.domain.service.UrlShortenerService;
import org.pragmatica.http.server.routing.RequestContext;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;

/**
 * API Controller.
 * <p>
 * Contains gluing logic which connects external world (HTTP) with internal domain logic.
 */
public interface UrlShortenerController {
    UrlShortenerService service();

    default Promise<UrlShortenerResponse> shortenUrl(RequestContext context) {
        return context.fromJson(new TypeToken<UrlShortenerRequest>() {})    // Try to parse request body as JSON
                      .map(UrlShortenerRequest::toDomainRequest)            // if successful - transform to domain request
                      .toPromise()                                          // go async
                      .flatMap(service()::shortenUrl)                       // Call business logic
                      .map(UrlShortenerResponse::fromShortenedUrl)          // Transform response to API response
                      .mapError(HttpError::unprocessableEntity);            // Map domain errors to HTTP errors
    }
}