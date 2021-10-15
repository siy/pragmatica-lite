package org.pfj.http.example;

import org.pfj.http.server.WebError;
import org.pfj.http.server.WebServer;

import java.nio.charset.StandardCharsets;

import static org.pfj.lang.Result.failure;
import static org.pfj.lang.Result.success;

public class App {
    public static void main(final String[] args) throws Exception {
        WebServer.create()
            .getText("/hello", (request) -> success("Hello world"))
            .postText("/hello", (request) -> success("Hello world: " + request.content().toString(StandardCharsets.UTF_8)))
            .getText("/boom", (request) -> {
                throw new RuntimeException("asdf");
            })
            .getText("/boom2", (request) -> failure(WebError.UNPROCESSABLE_ENTITY))
            .getText("/getbody", (request) -> success("What is this? " + request.content().toString(StandardCharsets.UTF_8)))
            .run();
    }
}
