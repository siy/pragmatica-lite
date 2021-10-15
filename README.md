# netty-example

Example web server using Netty, inspired by [Spark](https://github.com/perwendel/spark)

Example
-------

```java

public class App {
    public static void main(final String[] args) throws Exception {
        new WebServer()

            // Simple GET request
            .get("/hello", (request, response) -> "Hello world")

            // Simple POST request
            .post("/hello", (request, response) -> {
                return "Hello world: " + request.body();
            })

            // Start the server
            .start();
    }
}
```
