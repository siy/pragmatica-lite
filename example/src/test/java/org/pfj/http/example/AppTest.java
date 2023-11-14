package org.pfj.http.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.pfj.http.server.WebServer;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private static final WebServer server = App.buildServer();
    private static final Promise<Void> serverPromise = server.start();

    @AfterAll
    static void waitServer() {
        serverPromise.async(promise -> promise.resolve(Result.success(null))).join();
    }

    @Test
    void hello1EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/hello1")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("Hello world! /hello1/"));
    }

    @Test
    void hello2EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/hello2")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("Hello world! /hello2/"));
    }

    @Test
    void hello3EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/hello3")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("Hello world! /hello3/"));
    }

    @Test
    void hello4EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/hello4")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("Hello world! /hello4/"));
    }

    @Test
    void boomFunctionalEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/boom-functional")
            .then()
            .statusCode(422)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("Unprocessable Entity"));
    }

    @Test
    void boomLegacyEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/boom-legacy")
            .then()
            .statusCode(500)
            .contentType("text/plain; charset=UTF-8")
            .body(startsWith("java.lang.RuntimeException: Some exception message"));
    }

    @Test
    void delayEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/delay")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("1"));

        given().baseUri("http://localhost:8000")
            .get("/delay")
            .then()
            .statusCode(200)
            .contentType("text/plain; charset=UTF-8")
            .body(equalTo("2"));
    }

    @Test
    void userProfileEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
            .get("/v1/user/profile")
            .then()
            .statusCode(200)
            .contentType("application/json; charset=UTF-8")
            .body("first", equalTo("John"))
            .body("last", equalTo("Doe"))
            .body("email", equalTo("john.doe@gmail.com"));
    }

    /*
# 2021-10-26T11:11:31,954 [INFO/RoutingTable/main] - Route: GET: /v1/user/list/, contentType=APPLICATION_JSON
# 2021-10-26T11:11:31,954 [INFO/RoutingTable/main] - Route: GET: /v1/user/query/, contentType=APPLICATION_JSON
     */
}
