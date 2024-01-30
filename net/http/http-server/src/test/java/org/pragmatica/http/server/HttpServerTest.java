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
import static org.pragmatica.http.server.routing.PathParameter.*;
import static org.pragmatica.http.server.routing.PathParameter.aLong;
import static org.pragmatica.lang.Result.unitResult;

class HttpServerTest {
    private static final Logger log = LoggerFactory.getLogger(HttpServerTest.class);
    private static final HttpServer server = HttpServer.withConfig(HttpServerConfig.defaultConfiguration().withPort(8000))
                                                       .serve(Route.get("/one").toText(() -> "one"),
                                                              Route.get("/two").toText(() -> "two"),
                                                              Route.get("/three").toText(() -> "three"),
                                                              Route.patch("/one")
                                                                   .withPath(aString())
                                                                   .toValue(param1 -> STR."Received \{param1}")
                                                                   .asText(),
                                                              Route.patch("/two")
                                                                   .withPath(aInteger(), spacer("space"), aLong())
                                                                   .toValue((param1, param2, param3) -> STR."Received \{param1}, \{param2}, \{param3}")
                                                                   .asText());

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

    @Test
    void parametrizedRoutesAreWorking() {
        checkOneParametrizedPath("/one", 404, "Not Found: Unknown request path");
        checkOneParametrizedPath("/one/two", 200, "Received two");
        checkOneParametrizedPath("/one/two/", 200, "Received two");
        checkOneParametrizedPath("/one/two/three", 200, "Received two");
        checkOneParametrizedPath("/two", 404, "Not Found: Unknown request path");
        checkOneParametrizedPath("/two/111", 404, "Not Found: Unknown request path");
        checkOneParametrizedPath("/two/123/space", 404, "Not Found: Unknown request path");
        checkOneParametrizedPath("/two/123/space/456", 200, "Received 123, space, 456");
        checkOneParametrizedPath("/two/123/space/786/456", 200, "Received 123, space, 786");
        checkOneParametrizedPath("/two/123/space1/786", 404, "Not Found: Unknown request path");
        checkOneParametrizedPath("/two/a123/space/786", 422, "Unprocessable Entity: org.pragmatica.http.server.routing.PathParameter.lambda$aInteger$4(PathParameter.java:41)\n"
                                                             + "\tThe value [a123] can't be parsed into Integer: For input string: \"a123\"");
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

    private static void checkOneParametrizedPath(String path, int statusCode, String value) {
        given().baseUri("http://localhost:8000")
               .patch(path)
               .then()
               .log().status()
               .statusCode(statusCode)
               .contentType("text/plain; charset=UTF-8")
               .body(equalTo(value));
    }
}