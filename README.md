# Pragmatica Lite - Functional Style Micro Web Framework

Minimalistic web framework for Java 21+ with minimal dependencies.

## Features
* Functional style - no NPE, no exceptions, type safety, etc.
* Consistent Option/Result/Promise monads.
* Simple and convenient to use Promise-based asynchronous API - no low level technical details leaking into business logic.   
* Minimalistic - no annotations, no reflection, minimal external dependencies, only 3 main components: HttpServer, HttpClient and DB access layer.
* Fully asynchronous HTTP server and client, built-in caching domain name resolver with proper TTL handling.
* Minimal package size (example app jar is less than 7MB with all dependencies included). 

## Example 
Some examples can be found in the [example's](./examples) folder.

### Traditional Hello World application

```java
public class HelloWorld {
    public static void main(String[] args) {
        httpServerWith(defaultConfiguration().withPort(3000))
            .serveNow(
                handleGet("/").withText(() -> "Hello world!")
            );
    }
}
```

### Various routing examples

```java
    public static HttpServer buildServer() {
        return HttpServer
            .httpServerWith(HttpServerConfiguration.defaultConfiguration())
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
```
### PostgreSQL asynchronous CRUD Repository example
(actually, there is no update, but it's easy to guess how it will look like)

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
