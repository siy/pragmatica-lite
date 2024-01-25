package org.pragmatica.http.example.urlshortener.api;

import org.pragmatica.http.example.urlshortener.domain.api.external.ShortenedUrlResponse;

/**
 * Response model for the API.
 */
public record UrlShortenerResponse(String srcUrl, String shortenedUrl) {
    public static UrlShortenerResponse fromShortenedUrl(ShortenedUrlResponse shortenedUrl) {
        return new UrlShortenerResponse(shortenedUrl.srcUrl().trim(), shortenedUrl.shortenedUrl().trim());
    }
}
