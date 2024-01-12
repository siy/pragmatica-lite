package org.pragmatica.http.example.urlshortener.domain.api.external;

import org.pragmatica.http.example.urlshortener.domain.entity.ShortenedUrl;

/**
 * Response model for the domain logic. It transforms the internal domain representation to the public business logic API.
 * <p>
 * Note that transformation of the shortened URL ID into actual shortened URL is done here. The URL prefix is a constant, so it does not make sense to
 * store it in the database.
 */
public record ShortenedUrlResponse(String srcUrl, String shortenedUrl) {
    public static final String SHORTENED_URL_PREFIX = "http://test.short/";

    public static ShortenedUrlResponse fromDomainEntity(ShortenedUrl shortenedUrl) {
        return new ShortenedUrlResponse(shortenedUrl.srcUrl(), SHORTENED_URL_PREFIX + shortenedUrl.id());
    }
}
