# Pragmatica Lite - Micro Web Framework for Pragmatic Functional Java Coding Style

![License](https://img.shields.io/badge/license-Apache%202-blue.svg)

Minimalistic functional style micro web framework for Java 21+.

## Features
* Functional style - no NPE, no exceptions, type safety, etc.
* Option<T>/Result<T>/Promise<T> monads with consistent and compatible APIs.
* Simple and convenient to use Promise-based asynchronous API.   
* Minimalistic - no reflection, minimal external dependencies.
* Only 3 main fully asynchronous components: HttpServer, HttpClient and PostgreSQL DB driver.
* Built-in caching domain name resolver with proper TTL handling.

## Example 
Some examples can be found in the [examples](./examples) folder.

### Minimal Hello World application

```java
import org.pragmatica.http.server.routing.Route;

import static org.pragmatica.http.server.HttpServer.with;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    with(defaultConfiguration(),
         Route.get("/")
              .toText(() -> "Hello world!"));
}
```

### More Realistic App Skeleton 
This version loads configuration from the file and is ready for adding more routes.

```java
public class HelloWorld {
    public static void main(String[] args) {
        appConfig("server", HttpServerConfig.template())
            .flatMap(configuration -> HttpServer.with(configuration,
                                                      Route.get("/")
                                                           .toText(() -> "Hello world!")));
    }
}
```

### Various routing examples

```java
import java.util.stream.Stream;

public class RouteConfig implements RouteSource {
    public Stream<Route> routes() {
        return Stream.of(
            //Full description
            Route.get("/hello1")
                 .withoutParameters()
                 .to(request -> successful("Hello world! at " + request.route().path()))
                 .as(CommonContentTypes.TEXT_PLAIN),

            //Assume no parameters
            Route.get("/hello2")
                 .to(request -> successful("Hello world! at " + request.route().path()))
                 .as(CommonContentTypes.TEXT_PLAIN),

            //Assume no parameters, short content type (text)
            Route.get("/hello2")
                 .to(request -> successful("Hello world! at " + request.route().path()))
                 .asText(),

            //Assume no parameters, even shorter content type (json)
            Route.get("/hello2")
                 .to(request -> successful("Hello world! at " + request.route().path())),

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
            Route.<NanoId, Unit>get("/delay")
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
                 ));
    }
}

```
### PostgreSQL asynchronous CRUD Repository example
(actually, there is no Update implementation)

```java
public interface ShortenedUrlRepository {
    default Promise<ShortenedUrl> create(ShortenedUrl shortenedUrl) {
        return QRY."INSERT INTO shortenedurl (\{template().fieldNames()}) VALUES (\{template().fieldValues(shortenedUrl)}) RETURNING *"
            .in(db())
            .asSingle(template());
    }

    default Promise<ShortenedUrl> read(String id) {
        return QRY."SELECT * FROM shortenedurl WHERE id = \{id}"
            .in(db())
            .asSingle(template());
    }

    default Promise<Unit> delete(String id) {
        return QRY."DELETE FROM shortenedurl WHERE id = \{id}"
            .in(db())
            .asUnit();
    }

    DbEnv db();
}
```
