package org.pragmatica.http.example;

public record UrlShortenerResponse(String srcUrl, String shortenedUrl) {
    public static UrlShortenerResponse fromShortenedUrl(ShortenedUrl shortenedUrl) {
        return new UrlShortenerResponse(shortenedUrl.srcUrl(), shortenedUrl.id());
    }
}
