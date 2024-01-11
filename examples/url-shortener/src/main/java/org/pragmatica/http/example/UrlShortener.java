package org.pragmatica.http.example;


import org.pragmatica.http.server.routing.RequestContext;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.HttpServerConfiguration.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.get;

public class UrlShortener {
    private static final int MAX_URL_LENGTH = 250;
    private static final int ID_LENGTH = 10;

    public static void main(String[] args) {
        new UrlShortener().run(args);
    }

    private void run(String[] args) {
        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                get("/shorten").with(this::shortenUrl).asText()
            );
    }

    private Promise<UrlShortenerResponse> shortenUrl(RequestContext context) {
        return context.fromJson(new TypeToken<UrlShortenerRequest>() {})
                      .flatMap(this::validateRequest)
                      .toPromise()
                      .flatMap(this::shortenAndStoreUrl);
    }

    private Promise<UrlShortenerResponse> shortenAndStoreUrl(UrlShortenerRequest urlShortenerRequest) {
        var shortenedUrl = calculateShortUrl(urlShortenerRequest.srcUrl());



        return null;
    }

    private String calculateShortUrl(String source) {
        return STR."http://test.short/\{NanoId.secureNanoId(ID_LENGTH)}";
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