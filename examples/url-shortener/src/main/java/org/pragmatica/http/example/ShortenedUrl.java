package org.pragmatica.http.example;

import java.time.LocalDateTime;

public record ShortenedUrl(String id, String srcUrl, LocalDateTime created, LocalDateTime lastAccessed) {
}
