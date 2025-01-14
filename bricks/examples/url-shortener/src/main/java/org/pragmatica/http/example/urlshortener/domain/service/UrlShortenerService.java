package org.pragmatica.http.example.urlshortener.domain.service;


import org.pragmatica.http.example.urlshortener.domain.api.external.RawShortenUrlRequest;
import org.pragmatica.http.example.urlshortener.domain.api.external.ShortenedUrlResponse;
import org.pragmatica.http.example.urlshortener.domain.api.internal.ValidShortenUrlRequest;
import org.pragmatica.http.example.urlshortener.persistence.ShortenedUrlRepository;
import org.pragmatica.lang.Promise;

/**
 * Main business logic of the application.
 */
public interface UrlShortenerService {
    ShortenedUrlRepository repository();

    default Promise<ShortenedUrlResponse> shortenUrl(RawShortenUrlRequest request) {
        return request.parseRequest()
                      .map(ValidShortenUrlRequest::toDomainEntity)
            .onFailure(cause -> System.out.println("Failed to parse request: " + cause))
                      .toPromise()
                      .flatMap(repository()::create)
                      .map(ShortenedUrlResponse::fromDomainEntity);
    }
}