package org.pragmatica.http.example;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfiguration;
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

    public static HttpServer buildServer() {
        return HttpServer
            .with(HttpServerConfiguration.allDefaults().withPort(8000))
            .serve(
                //Full description
                get("/hello1")
                    .with(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .as(CommonContentTypes.TEXT_PLAIN),

                //Short content type (text)
                get("/hello2")
                    .with(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .asText(),

                //Runtime exception handling example
                get("/boom-legacy")
                    .with(_ -> {
                        throw new RuntimeException("Some exception message");
                    })
                    .asText(),

                //Functional error handling
                get("/boom-functional")
                    .with(_ -> failed(HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, "Test error")))
                    .asText(),

                //Long-running process
                get("/delay")
                    .with(_ -> delayedResponse())
                    .asText(),

                //Nested routes
                in("/v1")
                    .serve(
                        in("/user")
                            .serve(
                                get("/list")
                                    .with(request -> successful(request.pathParams()))
                                    .asJson(),
                                get("/query")
                                    .with(request -> successful(request.queryParams()))
                                    .asJson(),
                                get("/profile")
                                    .with(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                                    .asJson()
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
