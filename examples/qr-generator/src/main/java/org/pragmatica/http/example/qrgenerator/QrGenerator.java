package org.pragmatica.http.example.qrgenerator;

import org.pragmatica.http.ContentCategory;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.HttpServerConfigTemplate;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.type.TypeToken;

import static org.pragmatica.config.api.AppConfig.appConfig;
import static org.pragmatica.http.server.routing.Route.handlePost;

public class QrGenerator {
    public static final ContentType PNG_CONTENT_TYPE = ContentType.custom("image/png", ContentCategory.BINARY);

    public static void main(String[] args) {
        appConfig("server", HttpServerConfigTemplate.INSTANCE)
            .map(QrGenerator::startServer);
    }

    private static Result<Unit> startServer(HttpServerConfig configuration) {
        return HttpServer.withConfig(configuration)
                         .serveNow(route());
    }

    private static Route<byte[]> route() {
        return handlePost("/qr")
            .whereBodyIs(new TypeToken<QrRequest>() {})
            .with(QrGenerator::handler)
            .as(PNG_CONTENT_TYPE);
    }

    private static Promise<byte[]> handler(QrRequest qrRequest) {
        return QrGeneratorService.generateQR(qrRequest.urlToEmbed(), 512, 512)
                                 .toPromise();
    }
}
