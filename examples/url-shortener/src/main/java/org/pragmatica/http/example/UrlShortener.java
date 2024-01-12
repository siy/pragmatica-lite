package org.pragmatica.http.example;


import org.pragmatica.db.postgres.DbEnv;
import org.pragmatica.db.postgres.DbEnvConfig;
import org.pragmatica.http.server.routing.RequestContext;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.uri.IRI;

import java.time.LocalDateTime;

import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.post;
import static org.pragmatica.lang.Option.none;

//TODO: make configuration loadable
public class UrlShortener {
    private static final int MAX_URL_LENGTH = 250;
    private static final int ID_LENGTH = 10;

    private final ShortenedUrlRepository repository;

    private UrlShortener(String[] args) {
        var dbEnv = DbEnv.with(new DbEnvConfig(
            IRI.fromString("postgres://localhost:5432/urlshortener"),
            "urlshortener",
            "urlshortener",
            -1,
            10,
            false,
            none(),
            none()
        ));

        repository = () -> dbEnv;
    }

    public static void main(String[] args) {
        new UrlShortener(args).run();
    }

    private void run() {
        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                post("/shorten").with(this::shortenUrl).asText()
            );
    }

    private Promise<UrlShortenerResponse> shortenUrl(RequestContext context) {
        return context.fromJson(new TypeToken<UrlShortenerRequest>() {})
                      .flatMap(this::validateRequest)
                      .toPromise()
                      .flatMap(this::shortenAndStoreUrl);
    }

    private Promise<UrlShortenerResponse> shortenAndStoreUrl(UrlShortenerRequest urlShortenerRequest) {
        var shortenedUrl = STR."http://test.short/\{NanoId.secureNanoId(ID_LENGTH)}";

        var record = ShortenedUrlTemplate.builder()
                                         .id(shortenedUrl)
                                         .srcUrl(urlShortenerRequest.srcUrl())
                                         .created(LocalDateTime.now())
                                         .lastAccessed(LocalDateTime.now());


        return repository.create(record)
                         .map(UrlShortenerResponse::fromShortenedUrl);
    }

    private Result<UrlShortenerRequest> validateRequest(UrlShortenerRequest urlShortenerRequest) {
        if (urlShortenerRequest.srcUrl() == null || urlShortenerRequest.srcUrl().isEmpty()) {
            return UrlShortenerError.missingInputUrl().result();
        }

        if (urlShortenerRequest.srcUrl().length() > MAX_URL_LENGTH) {
            return UrlShortenerError.inputUrlIsTooLong().result();
        }

        if (!urlShortenerRequest.srcUrl().startsWith("http://") && !urlShortenerRequest.srcUrl().startsWith("https://")) {
            return UrlShortenerError.onlyHttpSupported().result();
        }

        return Result.success(urlShortenerRequest);
    }
}