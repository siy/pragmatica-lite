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
import static org.pragmatica.http.server.HttpServer.withConfig;
import static org.pragmatica.http.server.HttpServerConfig.defaultConfiguration;
import static org.pragmatica.http.server.routing.Route.handleGet;

/**
 * Minimal version of "Hello world" example.
 */
public static void main(String[] args) {
    withConfig(defaultConfiguration())
        .serveNow(handleGet("/").withText(() -> "Hello world!"));
}
```

### More Realistic App Skeleton 
This version loads configuration from the file and is ready for adding more routes.

```java
public class HelloWorld {
    public static void main(String[] args) {
        appConfig("server", HttpServerConfigTemplate.INSTANCE)
            .flatMap(HelloWorld::runServer);
    }

    private static Result<Unit> runServer(HttpServerConfig configuration) {
        return HttpServer.with(configuration, route());
    }

    private static RouteSource route() {
        return handleGet("/")
            .withText(() -> "Hello world!");
    }
}
```

### Various routing examples

```java
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
                                handleGet("/list") // -> /v1/user/list
                                    .withJson(request -> successful(request.pathParams())),
                                handleGet("/query") // -> /v1/user/query
                                    .withJson(request -> successful(request.queryParams())),
                                handleGet("/profile") // -> /v1/user/profile
                                    .withJson(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com"))))),
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

    default ShortenedUrlTemplate template() {
        return ShortenedUrlTemplate.INSTANCE;
    }
}
```
