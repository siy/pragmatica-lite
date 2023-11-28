package org.pragmatica.http.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.pragmatica.lang.Unit.unitResult;

class WebServerTest {
    private static final Logger log = LoggerFactory.getLogger(WebServerTest.class);
    private static final WebServer server = WebServer.with(Configuration.allDefaults().withPort(8000))
                                                     .serve(Route.get("/one").textWith(() -> "one"),
                                                            Route.get("/two").textWith(() -> "two"),
                                                            Route.get("/three").textWith(() -> "three"));

    private static Promise<Unit> serverPromise;

    @BeforeAll
    public static void setup() {
        serverPromise = server.start();
        log.info("Server started");
    }

    @AfterAll
    public static void cleanup() {
        serverPromise.resolve(unitResult()).await().onResultDo(() -> log.info("Server stopped"));
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