package org.pragmatica.http.example.qrgenerator;

import org.pragmatica.http.ContentCategory;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.server.HttpServerConfigTemplate;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.type.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.config.api.AppConfig.defaultApplicationConfig;
import static org.pragmatica.http.server.HttpServer.httpServerWith;
import static org.pragmatica.http.server.routing.Route.handlePost;

public class QrGenerator {
    private static final Logger log = LoggerFactory.getLogger(QrGenerator.class);

    public static void main(String[] args) {
        defaultApplicationConfig()
            .load("server", HttpServerConfigTemplate.INSTANCE)
            .onFailure(cause -> log.error("Failed to load configuration {}", cause))
            .onSuccess(config -> httpServerWith(config)
                .serveNow(
                    handlePost("/qr")
                        .whereBodyIs(new TypeToken<QrRequest>() {})
                        .with(QrGenerator::handler)
                        .as(ContentType.custom("image/png", ContentCategory.BINARY))
                ));
    }

    private static Promise<byte[]> handler(QrRequest qrRequest) {
        return QrGeneratorService.generateQR(qrRequest.urlToEmbed(), 512, 512)
                                 .toPromise();
    }
}
