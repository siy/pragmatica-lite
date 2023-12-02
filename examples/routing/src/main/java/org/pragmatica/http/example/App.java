package org.pragmatica.http.example;

import org.pragmatica.http.error.WebError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.WebServer;
import org.pragmatica.http.server.config.WebServerConfiguration;
import org.pragmatica.lang.Promise;

import java.util.concurrent.atomic.AtomicInteger;

import static org.pragmatica.http.server.routing.Route.get;
import static org.pragmatica.http.server.routing.Route.in;
import static org.pragmatica.lang.Promise.failed;
import static org.pragmatica.lang.Promise.successful;

public class App {
    public static void main(final String[] args) {
        buildServer()
            .start()
            .await();
    }

    public static WebServer buildServer() {
        return WebServer
            .with(WebServerConfiguration.allDefaults().withPort(8000))
            .serve(
                //Full description
                get("/hello1")
                    .textWith(request -> successful("Hello world! at " + request.route().path())),

                //Default content type (text)
                get("/hello2")
                    .with(request -> successful("Hello world! at " + request.route().path())),

                //Runtime exception handling example
                get("/boom-legacy")
                    .with(_ -> {
                        throw new RuntimeException("Some exception message");
                    }),

                //Functional error handling
                get("/boom-functional")
                    .with(_ -> failed(WebError.from(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                get("/delay")
                    .with(_ -> delayedResponse()),

                //Nested routes
                in("/v1")
                    .serve(
                        in("/user")
                            .serve(
                                get("/list")
                                    .jsonWith(request -> successful(request.pathParams())),
                                get("/query")
                                    .jsonWith(request -> successful(request.queryParams())),
                                get("/profile")
                                    .jsonWith(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
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
