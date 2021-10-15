package org.pfj.http.example;

import org.pfj.http.server.Route;
import org.pfj.http.server.WebError;
import org.pfj.http.server.WebServer;
import org.pfj.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pfj.http.server.Route.getText;
import static org.pfj.http.server.Route.postText;
import static org.pfj.lang.Promise.failure;
import static org.pfj.lang.Promise.success;

public class App {
    public static void main(final String[] args) throws Exception {
        WebServer.create(
            getText("/hello", (request) -> success("Hello world")),
            postText("/hello", (request) -> success("Hello world: " + request.bodyAsString())),
            getText("/boom", (request) -> {
                throw new RuntimeException("Some exception message");
            }),
            getText("/boom2", (request) -> failure(WebError.UNPROCESSABLE_ENTITY)),
            getText("/getbody", (request) -> success("What is this? " + request.bodyAsString())),
            getText("/delay", (request) -> delayedResponse()),
            Route.from("/v1",
                Route.from("/user",
                    getText("/list", (request -> success("User list"))),
                    getText("/manager", (request -> success("Manager list")))
                )
            )
        ).run();
    }

    private static final AtomicInteger counter = new AtomicInteger();

    private static Promise<Integer> delayedResponse() {
        return Promise.<Integer>promise()
            .async(promise -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
                promise.succeed(counter.incrementAndGet());
            });
    }
}
