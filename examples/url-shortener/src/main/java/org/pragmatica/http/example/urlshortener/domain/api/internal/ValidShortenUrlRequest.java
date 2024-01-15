package org.pragmatica.http.example.urlshortener.domain.api.internal;

import org.pragmatica.http.example.urlshortener.domain.entity.ShortenedUrl;
import org.pragmatica.http.example.urlshortener.persistence.template.ShortenedUrlTemplate;
import org.pragmatica.id.nanoid.NanoId;

import java.time.LocalDateTime;

/**
 * Represents a valid request to shorten a URL. Instances of this record are created only when it is known that the request is valid. This eliminates
 * need to check validity of the request in the business logic.
 */
public record ValidShortenUrlRequest(String srcUrl) {
    public ShortenedUrl toDomainEntity() {
        return ShortenedUrlTemplate.builder()
                                   .id(NanoId.secureNanoId(ShortenedUrl.ID_LENGTH))
                                   .srcUrl(srcUrl())
                                   .created(LocalDateTime.now())
                                   .lastAccessed(LocalDateTime.now());
    }
}
