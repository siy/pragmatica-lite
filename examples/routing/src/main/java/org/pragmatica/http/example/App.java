package org.pragmatica.http.example;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;

import static org.pragmatica.http.server.routing.Route.handleGet;
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
            .withConfig(HttpServerConfig.defaultConfiguration())
            .serve(
                //Full description
                handleGet("/hello1")
                    .withoutParameters()
                    .with(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .as(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters
                handleGet("/hello2")
                    .with(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .as(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters, short content type (text)
                handleGet("/hello2")
                    .with(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .asText(),

                //Assume no parameters, even shorter content type (json)
                handleGet("/hello2")
                    .withText(request -> successful(STR."Hello world! at \{request.route().path()}")),

                //Assume no parameters, response does not depend on request
                handleGet("/hello2")
                    .withText(() -> "Hello world!"),

                //Runtime exception handling example
                handleGet("/boom-legacy")
                    .withText(_ -> {
                        throw new RuntimeException("Some exception message");
                    }),

                //Functional error handling
                handleGet("/boom-functional")
                    .withText(_ -> failed(HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                handleGet("/delay")
                    .withText(_ -> delayedResponse()),

                //Nested routes
                in("/v1")
                    .serve(
                        in("/user")
                            .serve(
                                handleGet("/list")
                                    .withJson(request -> successful(request.pathParams())),
                                handleGet("/query")
                                    .withJson(request -> successful(request.queryParams())),
                                handleGet("/profile")
                                    .withJson(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                            )
                    )
            );
    }

    private static Promise<String> delayedResponse() {
        return Promise.promise(promise -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                //ignore
            }
            promise.succeed(NanoId.secureNanoId());
        });
    }
}
