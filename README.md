# Pragmatica Lite - Functional Style Micro Web Framework

Minimalistic web framework for Java 21+ with minimal dependencies.

## Features
* Functional style - no NPE, no exceptions, type safety, etc.
* Consistent Option/Result/Promise monads.
* Simple and convenient to use Promise-based asynchronous API - no low level technical details leaking into business logic.   
* Minimalistic - no annotations, no reflection, no code generation.
* Minimal external dependencies.
* Minimalistic API - only 3 main components: HttpServer, HttpClient (WIP) and DB access layer (WIP).
* Fully asynchronous HTTP server and client, built-in caching domain name resolver with proper TTL handling.
* Minimal package size (example app jar is about 6.5MB with all dependencies included). 

## Example 
Test app which demonstrates available routing configuration options. (WARNING: Subject to change!)

```java
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
```
