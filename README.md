# Pragmatica Lite - Micro Web Framework for Pragmatic Functional Java Coding Style

![License](https://img.shields.io/badge/license-Apache%202-blue.svg)

Minimalistic functional style micro web framework for Java 21+.

## Features
* Functional style - no NPE, no exceptions, type safety, etc.
* Consistent Option/Result/Promise monads with compatible APIs.
* Simple and convenient to use Promise-based asynchronous API.   
* Minimalistic - no reflection, minimal external dependencies.
* Only 3 main fully asynchronous components: HttpServer, HttpClient and PostgreSQL DB driver.
* Built-in caching domain name resolver with proper TTL handling.

## Example 
Some examples can be found in the [examples](./examples) folder.

### Minimal Hello World application

```java
import static org.pragmatica.http.server.HttpServer.with;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.whenGet;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    with(defaultConfiguration(),
         whenGet("/")
             .returnText(() -> "Hello world!"));
}
```

### More Realistic App Skeleton 
This version loads configuration from the file and is ready for adding more routes.

```java
public class HelloWorld {
    public static void main(String[] args) {
        appConfig("server", HttpServerConfig.template())
            .flatMap(configuration -> HttpServer.with(configuration,
                                                      whenGet("/")
                                                          .returnText(() -> "Hello world!")));
    }
}
```

### Various routing examples

```java
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
```
### PostgreSQL asynchronous CRUD Repository example
(actually, there is no Update implementation)

```java
public interface ShortenedUrlRepository {
    default Promise<ShortenedUrl> create(ShortenedUrl shortenedUrl) {
        return QRY."INSERT INTO shortenedurl (\{template().fieldNames()}) VALUES (\{template().fieldValues(shortenedUrl)}) RETURNING *"
            .in(dbEnv())
            .mapResult(ra -> ra.asSingle(template()));
    }

    default Promise<ShortenedUrl> read(String id) {
        return QRY."SELECT * FROM shortenedurl WHERE id = \{id}"
            .in(dbEnv())
            .mapResult(ra -> ra.asSingle(template()));
    }

    default Promise<Unit> delete(String id) {
        return QRY."DELETE FROM shortenedurl WHERE id = \{id}"
            .in(dbEnv())
            .mapToUnit();
    }

    DbEnv dbEnv();
}
```
