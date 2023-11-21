package org.pragmatica.http.example;

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
        return WebServer.with(Configuration.allDefaults())
                        .and(
                            //Full description
                            from("/hello1")
                                .get()
                                .text()
                                .with(request -> successful("Hello world! " + request.route().path())),

                            //Default content type (text)
                            from("/hello2")
                                .get()
                                .with(request -> successful("Hello world! " + request.route().path())),

                            //Shortcut for method, explicit content type
                            get("/hello3")
                                .text()
                                .with(request -> successful("Hello world! " + request.route().path())),

                            //Shortcut for method, default content type
                            get("/hello4")
                                .with(request -> successful("Hello world! " + request.route().path())),

                            //Runtime exception handling example
                            get("/boom-legacy").with(_ -> {
                                throw new RuntimeException("Some exception message");
                            }),

                            //Functional error handling
                            get("/boom-functional")
                                .with(_ -> failed(WebError.UNPROCESSABLE_ENTITY)),

                            //Long-running process
                            get("/delay")
                                .with(_ -> delayedResponse()),

                            //Nested routes
                            from(
                                "/v1",
                                from(
                                    "/user",
                                    get("/list")
                                        .json()
                                        .with(request -> successful(request.pathParams())),
                                    get("/query")
                                        .json()
                                        .with(request -> successful(request.queryParams())),
                                    get("/profile")
                                        .json()
                                        .with(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                                )
                            )
                        )
                        .build();
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
