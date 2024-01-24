package org.pragmatica.http.example.urlshortener.domain.entity;

import org.pragmatica.annotation.Template;

import java.time.LocalDateTime;

/**
 * Domain object representing a shortened URL.
 * <p>
 * Note that this object is also used as DTO for the interfacing with persistence layer and caller.
 * For such a simple use case this is acceptable, but in more complex scenarios it is better to have
 * separate DTOs and domain objects. Also, for now it has annotation {@link Template} which is used
 * by persistence layer. It will be removed once annotation processor will be supporting inclusion mode
 * to keep domain layer clean from any framework-specific dependencies.
 *
 * @param id - unique identifier of the shortened URL
 * @param srcUrl - original URL
 * @param created - creation timestamp
 * @param lastAccessed - last access timestamp
 */
@Template
public record ShortenedUrl(String id, String srcUrl, LocalDateTime created, LocalDateTime lastAccessed) {
    /**
     * Maximum length of the original URL.
     */
    public static final int MAX_URL_LENGTH = 250;

    /**
     * Length of the shortened URL.
     */
    public static final int ID_LENGTH = 10;

    public static ShortenedUrlTemplate template() {
        return ShortenedUrlTemplate.INSTANCE;
    }
}
