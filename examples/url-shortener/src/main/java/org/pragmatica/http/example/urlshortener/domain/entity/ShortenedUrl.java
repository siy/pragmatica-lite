package org.pragmatica.http.example.urlshortener.domain.entity;

import java.time.LocalDateTime;

/**
 * Domain object representing a shortened URL.
 * <p>
 * Note that this object is also used as DTO for the interfacing with persistence layer and caller.
 * For such a simple use case this is acceptable, but in more complex scenarios it is better to have
 * separate DTOs and domain objects.
 *
 * @param id - unique identifier of the shortened URL
 * @param srcUrl - original URL
 * @param created - creation timestamp
 * @param lastAccessed - last access timestamp
 */
public record ShortenedUrl(String id, String srcUrl, LocalDateTime created, LocalDateTime lastAccessed) {
    /**
     * Maximum length of the original URL.
     */
    public static final int MAX_URL_LENGTH = 250;

    /**
     * Length of the shortened URL.
     */
    public static final int ID_LENGTH = 10;
}
