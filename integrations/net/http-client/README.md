# HTTP Client for Pragmatica Lite

Promise-based HTTP client wrapper around JDK HttpClient with typed error handling.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>http-client</artifactId>
    <version>0.11.1</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.http.HttpOperations;
import org.pragmatica.http.JdkHttpOperations;
import java.net.http.HttpRequest;
import java.net.URI;

var http = JdkHttpOperations.jdkHttpOperations();

var request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .GET()
    .build();

http.sendString(request)
    .onSuccess(result -> {
        if (result.isSuccess()) {
            System.out.println("Response: " + result.body());
        } else {
            System.out.println("HTTP Error: " + result.statusMessage());
        }
    })
    .onFailure(error -> System.err.println("Request failed: " + error.message()));
```

## API Reference

### HttpOperations

Interface for HTTP operations returning `Promise<HttpResult<T>>`.

```java
// Create with default settings
HttpOperations http = JdkHttpOperations.jdkHttpOperations();

// Create with custom HttpClient
HttpOperations http = JdkHttpOperations.jdkHttpOperations(customHttpClient);
```

### Sending Requests

```java
// String response
Promise<HttpResult<String>> response = http.sendString(request);

// Byte array response
Promise<HttpResult<byte[]>> response = http.sendBytes(request);

// Discarding body (HEAD requests, etc.)
Promise<HttpResult<Void>> response = http.sendDiscarding(request);

// Custom body handler
Promise<HttpResult<Path>> response = http.send(request, BodyHandlers.ofFile(path));
```

### HttpResult

Typed response wrapper with status, headers, and body.

```java
http.sendString(request)
    .onSuccess(result -> {
        // Check status
        if (result.isSuccess()) {        // 2xx
            processSuccess(result.body());
        } else if (result.isClientError()) {  // 4xx
            handleClientError(result);
        } else if (result.isServerError()) {  // 5xx
            handleServerError(result);
        }

        // Access headers
        result.header("Content-Type").onPresent(ct -> log.info("Content-Type: {}", ct));
        List<String> cookies = result.headerValues("Set-Cookie");

        // Convert to Result (fails on non-2xx)
        result.toResult()
            .onSuccess(body -> process(body))
            .onFailure(error -> log.error("HTTP {}: {}", result.statusCode(), error.message()));
    });
```

### Error Handling

All network errors are mapped to typed `HttpError` causes:

```java
http.sendString(request)
    .onFailure(error -> {
        switch (error) {
            case HttpError.ConnectionFailed e ->
                log.error("Connection failed: {}", e.message());
            case HttpError.Timeout e ->
                log.warn("Request timed out: {}", e.message());
            case HttpError.RequestFailed e ->
                log.error("HTTP {}: {}", e.statusCode(), e.reason());
            case HttpError.InvalidResponse e ->
                log.error("Invalid response: {}", e.message());
            case HttpError.Failure e ->
                log.error("Unexpected error", e.cause());
        }
    });
```

## Complete Examples

### GET Request with JSON Parsing

```java
public Promise<User> fetchUser(long userId) {
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/users/" + userId))
        .header("Accept", "application/json")
        .GET()
        .build();

    return http.sendString(request)
        .flatMap(result -> result.toResult().async())
        .flatMap(body -> jsonMapper.readString(body, User.class).async());
}
```

### POST Request with Body

```java
public Promise<Order> createOrder(OrderRequest orderRequest) {
    return jsonMapper.writeAsString(orderRequest)
        .async()
        .flatMap(json -> {
            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            return http.sendString(request);
        })
        .flatMap(result -> result.toResult().async())
        .flatMap(body -> jsonMapper.readString(body, Order.class).async());
}
```

### Parallel Requests

```java
public Promise<Dashboard> loadDashboard(UserId userId) {
    return Promise.all(
        fetchUser(userId.value()),
        fetchOrders(userId.value()),
        fetchNotifications(userId.value())
    ).map(Dashboard::new);
}
```

### With Retry

```java
var retryPolicy = Retry.create()
    .attempts(3)
    .strategy(BackoffStrategy.exponential()
        .initialDelay(TimeSpan.timeSpan(100).millis())
        .maxDelay(TimeSpan.timeSpan(5).seconds())
        .factor(2.0));

public Promise<User> fetchUserWithRetry(long userId) {
    return retryPolicy.execute(() -> fetchUser(userId));
}
```

### With Timeout

```java
public Promise<User> fetchUserWithTimeout(long userId) {
    return fetchUser(userId)
        .timeout(Duration.ofSeconds(5), HttpError.timeout("User fetch timed out"));
}
```

## Error Types

| Error Type | Description |
|------------|-------------|
| `ConnectionFailed` | Network unreachable, DNS failure, connection refused |
| `Timeout` | Connection or request timeout exceeded |
| `RequestFailed` | HTTP error status code (4xx, 5xx) |
| `InvalidResponse` | Response parsing failed |
| `Failure` | Unexpected error (catch-all) |

## Dependencies

- JDK 11+ HttpClient (built-in)
- `pragmatica-lite-core`
