package org.pragmatica.http.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.pragmatica.http.server.WebServer;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.pragmatica.lang.Unit.unitResult;

/**
 * Unit test for simple App.
 */
public class AppTest {
    private static final WebServer server = App.buildServer();
    private static final Promise<Unit> serverPromise = server.start();

    @AfterAll
    static void waitServer() {
        serverPromise.async(promise -> promise.resolve(unitResult())).await();
    }

    @Test
    void hello1EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/hello1")
               .then()
               .statusCode(200)
               .contentType("text/plain; charset=UTF-8")
               .body(equalTo("Hello world! at /hello1/"));
    }

    @Test
    void hello2EndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/hello2")
               .then()
               .statusCode(200)
               .contentType("text/plain; charset=UTF-8")
               .body(equalTo("Hello world! at /hello2/"));
    }

    @Test
    void boomFunctionalEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/boom-functional")
               .then()
               .statusCode(422)
               .contentType("text/plain; charset=UTF-8")
               .body(equalTo("Unprocessable Entity: Test error"));
    }

    @Test
    void boomLegacyEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/boom-legacy")
               .then()
               .statusCode(500)
               .contentType("text/plain; charset=UTF-8")
               .body(startsWith("Internal Server Error: java.lang.RuntimeException: Some exception message"));
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

    @Test
    void userListEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/v1/user/list/one/two/three")
               .then()
               .statusCode(200)
               .contentType("application/json; charset=UTF-8")
               .body(equalTo("[\"one\",\"two\",\"three\"]"));
    }

    @Test
    void userQueryEndpointIsWorking() {
        given().baseUri("http://localhost:8000")
               .get("/v1/user/query?one=1&two=2&three=3&three=4")
               .then()
               .statusCode(200)
               .contentType("application/json; charset=UTF-8")
               .body(equalTo("{\"one\":[\"1\"],\"two\":[\"2\"],\"three\":[\"3\",\"4\"]}"));
    }
}
