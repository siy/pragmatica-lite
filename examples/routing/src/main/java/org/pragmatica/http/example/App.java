package org.pragmatica.http.example;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;

import static org.pragmatica.http.server.routing.Route.*;
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
                whenGet("/hello1")
                    .withoutParameters()
                    .returnFrom(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .a(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters
                whenGet("/hello2")
                    .returnFrom(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .a(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters, short content type (text)
                whenGet("/hello2")
                    .returnFrom(request -> successful(STR."Hello world! at \{request.route().path()}"))
                    .text(),

                //Assume no parameters, even shorter content type (json)
                whenGet("/hello2")
                    .returnText(request -> successful(STR."Hello world! at \{request.route().path()}")),

                //Assume no parameters, response does not depend on request
                whenGet("/hello2")
                    .returnText(() -> "Hello world!"),

                //Runtime exception handling example
                whenGet("/boom-legacy")
                    .returnText(_ -> {
                        throw new RuntimeException("Some exception message");
                    }),

                //Functional error handling
                whenGet("/boom-functional")
                    .returnText(_ -> failed(HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                whenGet("/delay")
                    .returnText(_ -> delayedResponse()),

                //Nested routes
                in("/v1")
                    .serve(
                        in("/user")
                            .serve(
                                whenGet("/list")
                                    .returnJson(request -> successful(request.pathParams())),
                                whenGet("/query")
                                    .returnJson(request -> successful(request.queryParams())),
                                whenGet("/profile")
                                    .returnJson(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
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
