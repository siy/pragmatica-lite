package org.pragmatica.http.example.urlshortener.domain.api.external;

import org.pragmatica.http.example.urlshortener.domain.api.internal.ValidShortenUrlRequest;
import org.pragmatica.http.example.urlshortener.domain.entity.ShortenedUrl;
import org.pragmatica.http.example.urlshortener.domain.error.UrlShortenerError;
import org.pragmatica.lang.Result;
import org.pragmatica.uri.IRI;

/**
 * Represents a input request to shorten a URL. Might be invalid. Method {@link #parseRequest()} transforms it into a {@link ValidShortenUrlRequest},
 * if input is valid.
 * <p>
 * Note that this approach uses <a href="https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/">Parse, don't validate</a> approach, to preserve
 * obtained information. This enables use of the parsed object throughout the application, without the need to validate it.
 */
public record RawShortenUrlRequest(String srcUrl) {
    public Result<ValidShortenUrlRequest> parseRequest() {
        if (srcUrl() == null || srcUrl().isEmpty()) {
            return new UrlShortenerError.MissingInputUrl("Missing input URL").result();
        }

        if (srcUrl().length() > ShortenedUrl.MAX_URL_LENGTH) {
            return new UrlShortenerError.InputUrlIsTooLong(STR."Input URL is too long \{this
                .srcUrl()
                .length()} > \{ShortenedUrl.MAX_URL_LENGTH}").result();
        }

        return IRI.fromString(srcUrl())
                  .scheme()
                  .filter(scheme -> scheme.equals("http") || scheme.equals("https"))
                  .toResult(new UrlShortenerError.OnlyHttpSupported("Only HTTP/HTTPS URLs are supported"))
                  .replace(() -> new ValidShortenUrlRequest(srcUrl()));
    }
}
