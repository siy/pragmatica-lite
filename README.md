# Pragmatica Lite - Functional Style Micro Web Framework

Minimalistic web framework for Java 21+ with minimal dependencies.

## Features
* Functional style - no NPE, no exceptions. Consistent Option/Result/Promise monads.
* Minimalistic - no annotations, no reflection, no code generation.
* Minimal dependencies - only slf4j-api and jackson-databind (for now, later expected some DB-related dependencies).
* Minimalistic API - only 4 main elements: WebServer, WebClient (WIP), Async resolver (WIP) and DB access layer (WIP).
* Provides type safety as much as possible
* Minimal package size (example app jar is about 6.5MB with all dependencies included). 

## Example 
Test app which demonstrates available routing configuration options. (WARNING: Subject to change!)

```java
public class App {
    public static void main(final String[] args) {
        buildServer()
            .start()
            .join();
    }

    public static WebServer buildServer() {
        return WebServer
            .with(Configuration.allDefaults())
            .serve(
                //Full description
                from("/hello1")
                    .get()
                    .text()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Default content type (text)
                from("/hello2")
                    .get()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Shortcut for method, explicit content type
                get("/hello3")
                    .text()
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Shortcut for method, default content type
                get("/hello4")
                    .then(request -> successful("Hello world! at " + request.route().path())),

                //Runtime exception handling example
                get("/boom-legacy").then(_ -> {
                    throw new RuntimeException("Some exception message");
                }),

                //Functional error handling
                get("/boom-functional")
                    .then(_ -> failed(WebError.from(HttpStatus.UNPROCESSABLE_ENTITY, "Test error"))),

                //Long-running process
                get("/delay")
                    .then(_ -> delayedResponse()),

                //Nested routes
                from(
                    "/v1",
                    from(
                        "/user",
                        get("/list")
                            .json()
                            .then(request -> successful(request.pathParams())),
                        get("/query")
                            .json()
                            .then(request -> successful(request.queryParams())),
                        get("/profile")
                            .json()
                            .then(_ -> successful(new UserProfile("John", "Doe", "john.doe@gmail.com")))
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
