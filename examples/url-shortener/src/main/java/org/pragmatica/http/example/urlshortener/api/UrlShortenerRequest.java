package org.pragmatica.http.example.urlshortener.api;

import org.pragmatica.http.example.urlshortener.domain.api.external.RawShortenUrlRequest;

/**
 * Request model for the API.
 */
public record UrlShortenerRequest(String srcUrl) {
    public RawShortenUrlRequest toDomainRequest() {
        return new RawShortenUrlRequest(srcUrl());
    }
}
