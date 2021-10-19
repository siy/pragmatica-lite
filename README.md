# Pragmatica REST Example

Example nano web framework written in Pragmatic Functional Java style.

Example
-------

```java
public class App {
    public static void main(final String[] args) {
        WebServer.create(
            //Full description
            from("/hello").get().text().with(request -> success("Hello world")),

            //Default content type (text)
            from("/hello").get().with(request -> success("Hello world: " + request.bodyAsString())),

            //Shortcut for method, explicit content type
            get("/getbody").text().with(request -> success("What is this? " + request.bodyAsString())),

            //Shortcut for method, default content type
            get("/getbody").with(request -> success("What is this? " + request.bodyAsString())),

            //Error handling
            //a) Runtime exception handling example
            get("/boom").with(request -> {
                throw new RuntimeException("Some exception message");
            }),
            //b) Return error
            get("/boom2").with(request -> failure(WebError.UNPROCESSABLE_ENTITY)),

            //Nested routes
            from("/v1",
                from(
                    "/user",
                    get("/list").json().with(request -> success(request.pathParams())),
                    get("/query").json().with(request -> success(request.queryParams())),
                    get("/profile").json().with(request -> success(new UserProfile("John", "Doe", "john.doe@gmail.com")))
                )
            )
        )
        .start()
        .join();
    }
}
```
