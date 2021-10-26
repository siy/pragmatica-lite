package org.pfj.http.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pfj.http.server.WebServer;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;

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
        given().baseUri("http://localhost:8000").get("/hello1")
            .then()
            .contentType("text/plain; charset=UTF-8");
    }

    /*
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /boom-functional/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /boom-legacy/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /delay/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /hello1/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /hello2/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /hello3/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,953 [INFO/RoutingTable/main] - Route: GET: /hello4/, contentType=TEXT_PLAIN
# 2021-10-26T11:11:31,954 [INFO/RoutingTable/main] - Route: GET: /v1/user/list/, contentType=APPLICATION_JSON
# 2021-10-26T11:11:31,954 [INFO/RoutingTable/main] - Route: GET: /v1/user/profile/, contentType=APPLICATION_JSON
# 2021-10-26T11:11:31,954 [INFO/RoutingTable/main] - Route: GET: /v1/user/query/, contentType=APPLICATION_JSON

     */
}
