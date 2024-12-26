package org.pragmatica.http.example;

import org.pragmatica.http.CommonContentTypes;
import org.pragmatica.http.protocol.HttpStatus;
import org.pragmatica.http.server.HttpServer;
import org.pragmatica.http.server.HttpServerConfig;
import org.pragmatica.http.server.routing.Route;
import org.pragmatica.http.server.routing.RouteSource;
import org.pragmatica.id.nanoid.NanoId;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.Promise.success;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(@SuppressWarnings("unused") final String[] args) {
        HttpServer
            .withConfig(HttpServerConfig.defaultConfiguration())
            .serveNow(routes())
            .onResult(() -> log.info("Server stopped"));
    }

    public static RouteSource routes() {
        return RouteSource.of(
            //Full description
            Route.get("/hello1")
                 .withoutParameters()
                 .to(request -> success("Hello world! at " + request.route().path()))
                 .as(CommonContentTypes.TEXT_PLAIN),

            //Assume no parameters
            Route.get("/hello2")
                 .to(request -> success("Hello world! at " + request.route().path()))
                 .as(CommonContentTypes.TEXT_PLAIN),

            //Assume no parameters, short content type (text)
            Route.get("/hello2")
                 .to(request -> success("Hello world! at " + request.route().path()))
                 .asText(),

            //Assume no parameters, even shorter content type (json)
            Route.get("/hello2")
                 .toText(request -> success("Hello world! at " + request.route().path())),

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
                 .toText(_ -> HttpStatus.UNPROCESSABLE_ENTITY.with("Test error").promise()),

            //Long-running process
            Route.<NanoId, Unit>get("/delay")
                 .toText(_ -> delayedResponse()),

            //Nested routes
            Route.in("/v1")
                 .serve(
                     Route.in("/user")
                          .serve(
                              Route.get("/list")
                                   .toJson(request -> success(request.pathParams())),
                              Route.get("/query")
                                   .toJson(request -> success(request.queryParams())),
                              Route.get("/profile")
                                   .toJson(_ -> success(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                          )
                 )
        );
    }

    private static Promise<NanoId> delayedResponse() {
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
