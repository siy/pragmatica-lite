package org.pragmatica.http.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.pragmatica.lang.Result.unitResult;

class HttpServerTest {
    private static final Logger log = LoggerFactory.getLogger(HttpServerTest.class);
    private static final HttpServer server = HttpServer.httpServerWith(HttpServerConfig.defaultConfiguration().withPort(8000))
                                                       .serve(Route.handleGet("/one").withText(() -> "one"),
                                                              Route.handleGet("/two").withText(() -> "two"),
                                                              Route.handleGet("/three").withText(() -> "three"));

    private static Promise<Unit> serverPromise;

    @BeforeAll
    public static void setup() {
        serverPromise = server.start();
        log.info("Server started");
    }

    @AfterAll
    public static void cleanup() {
        serverPromise.resolve(unitResult()).await().onResult(() -> log.info("Server stopped"));
    }

    @Test
    void routesAreWorking() {
        checkOneTextPath("/one", "one");
        checkOneTextPath("/two", "two");
        checkOneTextPath("/three", "three");
    }

    private static void checkOneTextPath(String path, String value) {
        given().baseUri("http://localhost:8000")
               .get(path)
               .then()
               .log().status()
               .statusCode(200)
               .contentType("text/plain; charset=UTF-8")
               .body(equalTo(value));
    }
}