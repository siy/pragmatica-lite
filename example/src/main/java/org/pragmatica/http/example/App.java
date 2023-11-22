package org.pragmatica.http.example;

import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.WebServer;
import org.pragmatica.http.server.config.Configuration;
import org.pragmatica.http.server.error.WebError;
import org.pragmatica.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.http.server.routing.Route.from;
import static org.pragmatica.http.server.routing.Route.get;
import static org.pragmatica.lang.Promise.failed;
import static org.pragmatica.lang.Promise.successful;

public class App {
    public static void main(final String[] args) {
        buildServer()
            .start()
            .join();
    }

    public static WebServer buildServer() {
        return WebServer
            .with(Configuration.allDefaults())
            .serve(
                //Full description
                from("/hello1")
                    .get()
                    .text()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Default content type (text)
                from("/hello2")
                    .get()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Shortcut for method, explicit content type
                get("/hello3")
                    .text()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Shortcut for method, default content type
                get("/hello4")
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Runtime exception handling example
                get("/boom-legacy").then(_ -> {
                    throw new RuntimeException("Some exception message");
                }),

                //Functional error handling
                get("/boom-functional")
                    .then(_ -> failed(WebError.from(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                get("/delay")
                    .then(_ -> delayedResponse()),

                //Nested routes
                from(
                    "/v1",
                    from(
                        "/user",
                        get("/list")
                            .json()
                            .then(request -> successful(request.pathParams())),
                        get("/query")
                            .json()
                            .then(request -> successful(request.queryParams())),
                        get("/profile")
                            .json()
                            .then(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                    )
                )
            );
    }

    private static final AtomicInteger counter = new AtomicInteger();

    private static Promise<Integer> delayedResponse() {
        return Promise.<Integer>promise()
                      .async(promise -> {
                          try {
                              Thread.sleep(250);
                          } catch (InterruptedException e) {
                              //ignore
                          }
                          promise.success(counter.incrementAndGet());
                      });
    }
}
