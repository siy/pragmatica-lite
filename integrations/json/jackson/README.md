# Jackson Integration for Pragmatica Lite

Result-based JSON serialization/deserialization with Jackson 3.0.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jackson</artifactId>
    <version>0.9.8</version>
</dependency>
```

Jackson 3.0 dependencies are included transitively.

## Quick Start

```java
import org.pragmatica.json.JsonMapper;

var mapper = JsonMapper.defaultJsonMapper();

// Serialize to JSON
mapper.writeAsString(user)
    .onSuccess(json -> System.out.println(json))
    .onFailure(error -> log.error("Serialization failed: {}", error.message()));

// Deserialize from JSON
mapper.readString(json, User.class)
    .onSuccess(user -> processUser(user))
    .onFailure(error -> log.error("Deserialization failed: {}", error.message()));
```

## API Reference

### JsonMapper

All operations return `Result<T>` instead of throwing exceptions.

#### Creating a Mapper

```java
// Default mapper with Result/Option support
JsonMapper mapper = JsonMapper.defaultJsonMapper();

// Custom mapper with builder
JsonMapper mapper = JsonMapper.jsonMapper()
    .withPragmaticaTypes()  // Enables Result/Option serialization
    .configure(builder -> builder
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
    .withModule(customModule)
    .build();
```

#### Serialization

```java
// To String
Result<String> json = mapper.writeAsString(object);

// To byte array
Result<byte[]> bytes = mapper.writeAsBytes(object);
```

#### Deserialization

```java
// From String with Class
Result<User> user = mapper.readString(json, User.class);

// From String with TypeToken (for generics)
Result<List<User>> users = mapper.readString(json, new TypeToken<List<User>>() {});

// From byte array
Result<User> user = mapper.readBytes(bytes, User.class);
Result<List<User>> users = mapper.readBytes(bytes, new TypeToken<List<User>>() {});
```

## Pragmatica Type Serialization

### Result<T>

```json
// Success
{"success": true, "value": {"name": "Alice", "age": 30}}

// Failure
{"success": false, "error": {"message": "Validation failed", "type": "ValidationError"}}
```

```java
// Serialize
Result<User> result = Result.success(user);
mapper.writeAsString(result);  // {"success":true,"value":{...}}

// Deserialize
mapper.readString(json, new TypeToken<Result<User>>() {})
    .onSuccess(result ->
        result.onSuccess(user -> process(user))
              .onFailure(cause -> log.warn("Deserialized failure: {}", cause.message()))
    );
```

### Option<T>

```json
// Some
"value"  // or {"name": "Alice"} for objects

// None
null
```

```java
// In records
record UserProfile(String name, Option<String> nickname) {}

var profile = new UserProfile("Alice", Option.some("Ali"));
mapper.writeAsString(profile);
// {"name":"Alice","nickname":"Ali"}

var profile2 = new UserProfile("Bob", Option.none());
mapper.writeAsString(profile2);
// {"name":"Bob","nickname":null}
```

## Error Handling

All JSON errors are mapped to typed `JsonError` causes:

```java
mapper.readString(json, User.class)
    .onFailure(error -> {
        switch (error) {
            case JsonError.ParseFailed e ->
                log.error("Invalid JSON syntax: {}", e.message());
            case JsonError.MappingFailed e ->
                log.error("Cannot map to type: {}", e.message());
            case JsonError.SerializationFailed e ->
                log.error("Serialization error: {}", e.message());
        }
    });
```

## Complete Examples

### REST API Response Handling

```java
public class ApiClient {
    private final HttpOperations http;
    private final JsonMapper json;

    public Promise<User> fetchUser(long userId) {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.example.com/users/" + userId))
            .GET()
            .build();

        return http.sendString(request)
            .flatMap(response -> response.toResult().async())
            .flatMap(body -> json.readString(body, User.class).async());
    }

    public Promise<List<Order>> fetchOrders(long userId) {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.example.com/users/" + userId + "/orders"))
            .GET()
            .build();

        return http.sendString(request)
            .flatMap(response -> response.toResult().async())
            .flatMap(body -> json.readString(body, new TypeToken<List<Order>>() {}).async());
    }
}
```

### Request/Response Serialization

```java
public class OrderController {
    private final JsonMapper json;
    private final ProcessOrder processOrder;

    public Promise<byte[]> handleCreateOrder(byte[] requestBody) {
        return json.readBytes(requestBody, CreateOrderRequest.class)
            .async()
            .flatMap(processOrder::execute)
            .flatMap(response -> json.writeAsBytes(response).async());
    }
}
```

### Configuration with Result

```java
public Result<AppConfig> loadConfig(Path configPath) {
    try {
        var content = Files.readString(configPath);
        return json.readString(content, AppConfig.class);
    } catch (IOException e) {
        return ConfigError.READ_FAILED.result();
    }
}
```

### Handling Optional Fields

```java
public record CreateUserRequest(
    String name,
    String email,
    Option<String> phone,
    Option<Address> address
) {}

// JSON with all fields
{
    "name": "Alice",
    "email": "alice@example.com",
    "phone": "+1234567890",
    "address": {"street": "123 Main St", "city": "NYC"}
}

// JSON with optional fields as null
{
    "name": "Bob",
    "email": "bob@example.com",
    "phone": null,
    "address": null
}
```

## Jackson 3.0 Notes

This integration uses Jackson 3.0 with the following changes from 2.x:

| Jackson 2.x | Jackson 3.0 |
|-------------|-------------|
| `com.fasterxml.jackson.*` | `tools.jackson.*` |
| `JsonSerializer` | `ValueSerializer` |
| `JsonDeserializer` | `ValueDeserializer` |
| `SerializerProvider` | `SerializationContext` |
| Checked exceptions | Unchecked exceptions |

## Dependencies

- Jackson 3.0.0+ (`tools.jackson.core:jackson-databind`)
- `pragmatica-lite-core`
