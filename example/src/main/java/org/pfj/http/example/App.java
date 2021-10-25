package org.pfj.http.example;

import org.pfj.http.server.Configuration;
import org.pfj.http.server.error.WebError;
import org.pfj.http.server.WebServer;
import org.pfj.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pfj.http.server.routing.Route.from;
import static org.pfj.http.server.routing.Route.get;
import static org.pfj.lang.Promise.failure;
import static org.pfj.lang.Promise.success;

public class App {
    public static void main(final String[] args) {
		WebServer.with(Configuration.atDefaultPort().build())
            .and(
                //Full description
                from("/hello")
					.get().text().from(request -> success("Hello world")),

                //Default content type (text)
                from("/hello")
					.get().from(request -> success("Hello world: " + request.bodyAsString())),

                //Shortcut for method, explicit content type
                get("/getbody")
					.text().from(request -> success("What is this? " + request.bodyAsString())),

                //Shortcut for method, default content type
                get("/getbody")
					.from(request -> success("What is this? " + request.bodyAsString())),

                //Runtime exception handling example
                get("/boom").from(request -> {
                    throw new RuntimeException("Some exception message");
                }),

                //Error handling
                get("/boom2")
					.from(request -> failure(WebError.UNPROCESSABLE_ENTITY)),

                //Long-running process
                get("/delay")
					.from(request -> delayedResponse()),

                //Nested routes
                from(
                    "/v1",
                    from(
                        "/user",
                        get("/list").json().from(request -> success(request.pathParams())),
                        get("/query").json().from(request -> success(request.queryParams())),
                        get("/profile").json().from(request -> success(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                    )
                )
            )
            .build()
            .start()
            .join();
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
