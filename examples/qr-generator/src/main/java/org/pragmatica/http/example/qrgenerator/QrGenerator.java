package org.pragmatica.http.example.qrgenerator;

import org.pragmatica.http.ContentCategory;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Promise;

import static org.pragmatica.config.api.AppConfig.appConfig;
import static org.pragmatica.http.example.qrgenerator.QrGeneratorService.generateQR;

public class QrGenerator {
    public static final ContentType PNG_CONTENT_TYPE = ContentType.custom("image/png", ContentCategory.BINARY);

    public static void main(String[] args) {
        appConfig("server", HttpServerConfig.template())
            .map(configuration -> HttpServer.with(configuration, route()));
    }

    private static Route<byte[]> route() {
        return Route.<byte[], QrRequest>post("/qr")
                    .withBody(QrRequest.class)
                    .to(QrGenerator::handler)
                    .as(PNG_CONTENT_TYPE);
    }

    private static Promise<byte[]> handler(QrRequest qrRequest) {
        return generateQR(qrRequest.urlToEmbed(), 512, 512)
            .toPromise();
    }
}
