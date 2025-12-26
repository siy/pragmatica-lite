# Pragmatica Lite

![License](https://img.shields.io/badge/license-Apache%202-blue.svg)
![Java](https://img.shields.io/badge/Java-25-orange.svg)
![Maven Central](https://img.shields.io/badge/Maven-0.9.0-blue.svg)

## Modern Functional Programming for Java 25

Pragmatica Lite brings the power of functional programming to Java with zero-dependency monadic types that eliminate null pointer exceptions, unchecked exceptions, and callback hell. Built on Java 25's latest features including sealed interfaces and pattern matching.

## Why Pragmatica Lite?

**Without Pragmatica:**
```java
// Traditional Java - prone to NPE and unhandled exceptions
public User getUser(String id) throws UserNotFoundException, DatabaseException {
    User user = database.findUser(id);  // Can throw exception
    if (user == null) {                 // NPE risk
        throw new UserNotFoundException(id);
    }
    return user;
}
```

**With Pragmatica:**
```java
// Clean, safe, composable
public Result<User> getUser(String id) {
    return database.findUser(id)
        .filter(user -> user.isActive())
        .map(this::enrichUserData)
        .recover(DatabaseError.class, this::handleDatabaseError);
}
```

## Core Features

### Eliminate Null Pointer Exceptions
```java
Option<String> name = Option.of(user.getName())
    .map(String::toUpperCase)
    .filter(n -> n.length() > 2)
    .orElse("Anonymous");
```

### Safe Error Handling Without Exceptions
```java
Result<Integer> divide(int a, int b) {
    return b == 0
        ? Result.err(new MathError("Division by zero"))
        : Result.ok(a / b);
}

// Chain operations safely
Result<String> result = divide(10, 2)
    .map(x -> x * 2)
    .map(Object::toString)
    .recover(MathError.class, err -> "Error: " + err.message());
```

### Asynchronous Programming Made Simple
```java
Promise<UserProfile> getUserProfile(String userId) {
    return fetchUser(userId)
        .flatMap(user -> fetchPreferences(user.id()).map(prefs -> new UserProfile(user, prefs)))
        .onSuccess(profile -> cache.store(profile))
        .onFailure(error -> logger.error("Failed to load profile", error));
}
```

### Process Collections Safely
```java
List<Result<User>> userResults = userIds.stream()
    .map(this::fetchUser)
    .toList();

// Collect all successes, fail if any operation failed
Result<List<User>> allUsers = Result.allOf(userResults);

// Or collect only successful operations
List<User> validUsers = Result.collectSuccesses(userResults);
```

## Modern Java 25 Features

Pragmatica Lite leverages cutting-edge Java 25 features:

- **Sealed Interfaces**: Type-safe Result and Option hierarchies
- **Pattern Matching**: Elegant switch expressions for monadic types
- **Records**: Immutable data structures throughout

### Maven Configuration

Pragmatica Lite is available on Maven Central. Simply add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>core</artifactId>
    <version>0.9.0</version>
</dependency>
```

For Gradle users:

```gradle
implementation 'org.pragmatica-lite:core:0.9.0'
```

### Your First Pragmatica Application

```java
import org.pragmatica.lang.*;

public class Example {
    // Safe division that never throws
    public static Result<Double> safeDivide(double a, double b) {
        return b == 0.0
            ? MathError.divisionByZero().result()
            : Result.ok(a / b);
    }

    // Chaining operations
    static void main(String[] args) {
        safeDivide(10.0, 2.0)
            .map(result -> result * 2)  // Transform success value
            .fold(
                error -> "Error: " + error.message(),
                success -> "Result: " + success
            );
    }
}
```

## API Overview

### Result&lt;T&gt; - Railway-Oriented Programming
Handle success and failure without exceptions:

```java
Result<String> result = processData()
    .map(String::trim)
    .filter(s -> !s.isEmpty())
    .recover(ValidationError.class, err -> "default");
```

### Option&lt;T&gt; - Null Safety
Eliminate null pointer exceptions:

```java
Option<User> user = findUser(id)
    .filter(User::isActive)
    .map(User::getProfile);
```

### Promise&lt;T&gt; - Async Operations
Composable asynchronous programming:

```java
Promise<String> response = httpClient.get(url)
    .map(Response::body)
    .timeout(Duration.ofSeconds(5))
    .onFailure(err -> logger.error("Request failed", err));
```

## Design Principles

- **Zero Dependencies**: No external libraries required
- **Type Safety**: Leverage Java's type system for correctness
- **Performance**: Minimal overhead, allocation-free operations
- **Composability**: Chain operations naturally
- **Modern Java**: Built for Java 25 and beyond

## Examples and Documentation

Explore comprehensive examples in the [examples](examples) directory:
- Asynchronous data processing with Promise
- Error handling patterns with Result
- Null-safe operations with Option
- Real-world application patterns

## Module Structure

| Module | Description |
|--------|-------------|
| **core** | Core monadic types: Result, Option, Promise |
| **integrations/net/http-client** | Promise-based HTTP client (JDK HttpClient wrapper) |
| **integrations/db/jdbc** | Promise-based JDBC with HikariCP support |
| **integrations/db/r2dbc** | Promise-based R2DBC for reactive database access |
| **integrations/db/jooq-r2dbc** | Promise-based JOOQ with R2DBC |
| **integrations/db/jpa** | JPA integration with Promise-based operations |
| **integrations/json/jackson** | Jackson 3.0 integration for JSON serialization |
| **integrations/metrics/micrometer** | Micrometer metrics for Result/Option/Promise |
| **examples** | Sample applications and usage patterns |

## Integrations

### Jackson JSON (3.0.0)

Serialize and deserialize Result, Option, and Promise types:

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jackson</artifactId>
    <version>0.9.0</version>
</dependency>
```

```java
var mapper = JsonMapper.jsonMapper()
    .withPragmaticaTypes()
    .build();

// Result<T> serialization
Result<User> result = fetchUser(id);
mapper.writeAsString(result)  // Result<String>
    .onSuccess(json -> sendResponse(json));

// Type-safe deserialization
mapper.readString(json, new TypeToken<List<User>>() {})
    .onSuccess(users -> processUsers(users));
```

### JPA Database Integration

Promise-based JPA operations with typed errors:

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jpa</artifactId>
    <version>0.9.0</version>
</dependency>
```

```java
var ops = JpaOperations.jpaOperations(entityManager);

// Transactional operations
Promise<User> result = Transactional.withTransaction(
    entityManager,
    JpaError::fromException,
    tx -> ops.persist(newUser)
        .flatMap(saved -> ops.flush().map(_ -> saved))
);

// Typed errors: EntityNotFound, OptimisticLock, DatabaseFailure, etc.
result.onFailure(error -> switch (error) {
    case JpaError.EntityNotFound _ -> handleNotFound();
    case JpaError.OptimisticLock lock -> handleConflict(lock.entityType());
    case JpaError.DatabaseFailure db -> handleDatabaseError(db.cause());
    default -> handleUnexpected(error);
});
```

### Micrometer Metrics

Monitor Result, Option, and Promise operations:

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>micrometer</artifactId>
    <version>0.9.0</version>
</dependency>
```

```java
// Wrap operations with metrics
var wrappedOperation = ResultMetrics.timed(
    registry,
    "user.fetch",
    Tags.of("service", "user-service"),
    () -> fetchUser(userId)
);

// Automatic success/failure tracking
Promise<Data> monitored = PromiseMetrics.counted(
    registry,
    "api.call",
    Tags.of("endpoint", "/users"),
    apiClient.fetchData()
);
```

## Contributing

We welcome contributions! Please feel free to submit pull requests, report issues, or suggest improvements.

### Development Requirements
- Java 25 or higher
- Maven 3.9+

## Support

If you find this useful, consider [sponsoring](https://github.com/sponsors/siy).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
