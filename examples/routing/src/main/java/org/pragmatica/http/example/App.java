package org.pragmatica.http.example;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.HttpError;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

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
                Route.get("/hello1")
                     .withoutParameters()
                     .to(request -> successful(STR."Hello world! at \{request.route().path()}"))
                     .as(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters
                Route.get("/hello2")
                     .to(request -> successful(STR."Hello world! at \{request.route().path()}"))
                     .as(CommonContentTypes.TEXT_PLAIN),

                //Assume no parameters, short content type (text)
                Route.get("/hello2")
                     .to(request -> successful(STR."Hello world! at \{request.route().path()}"))
                     .asText(),

                //Assume no parameters, even shorter content type (json)
                Route.get("/hello2")
                     .toText(request -> successful(STR."Hello world! at \{request.route().path()}")),

                //Assume no parameters, response does not depend on request
                Route.get("/hello2")
                     .toText(() -> "Hello world!"),

                //Runtime exception handling example
                Route.get("/boom-legacy")
                     .toText(_ -> {
                         throw new RuntimeException("Some exception message");
                     }),

                //Functional error handling
                Route.get("/boom-functional")
                     .toText(_ -> failed(HttpError.httpError(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                Route.<String, Unit>get("/delay")
                     .toText(_ -> delayedResponse()),

                //Nested routes
                Route.in("/v1")
                     .serve(
                         Route.in("/user")
                              .serve(
                                  Route.get("/list")
                                       .toJson(request -> successful(request.pathParams())),
                                  Route.get("/query")
                                       .toJson(request -> successful(request.queryParams())),
                                  Route.get("/profile")
                                       .toJson(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
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
