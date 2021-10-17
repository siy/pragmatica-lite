package org.pfj.http.example;

import org.pfj.http.server.WebError;
import org.pfj.http.server.WebServer;
import org.pfj.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pfj.http.server.Route.at;
import static org.pfj.lang.Promise.failure;
import static org.pfj.lang.Promise.success;

public class App {
    public static void main(final String[] args) throws Exception {
        WebServer.create(
            at("/hello").get().text().from(request -> success("Hello world")),
            at("/hello").get().text().from(request -> success("Hello world: " + request.bodyAsString())),
            at("/boom").get().text().from(request -> {
                throw new RuntimeException("Some exception message");
            }),
            at("/boom2").get().text().from(request -> failure(WebError.UNPROCESSABLE_ENTITY)),
            at("/getbody").get().text().from(request -> success("What is this? " + request.bodyAsString())),
            at("/delay").get().text().from(request -> delayedResponse()),
            at("/v1",
                at("/user",
                    at("/list").get().text().from(request -> success("User list")),
                    at("/manager").get().text().from(request -> success("Manager list")),
                    at("/profile").get().json().from(request -> success(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                )
            )
        ).run();
    }

    private static final AtomicInteger counter = new AtomicInteger();

    private static Promise<Integer> delayedResponse() {
        return Promise.<Integer>promise()
            .async(promise -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //ignore
                }
                promise.succeed(counter.incrementAndGet());
            });
    }
}
