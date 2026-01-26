# R2DBC Integration for Pragmatica Lite

Promise-based reactive database access with typed error handling.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>r2dbc</artifactId>
    <version>0.11.0</version>
</dependency>
```

Requires R2DBC SPI and a driver:
```xml
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-spi</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.r2dbc.R2dbcOperations;
import io.r2dbc.spi.ConnectionFactories;

var connectionFactory = ConnectionFactories.get(
    "r2dbc:postgresql://localhost:5432/mydb"
);

R2dbcOperations r2dbc = R2dbcOperations.r2dbcOperations(connectionFactory);

// Query with row mapper
r2dbc.queryOne(
    "SELECT * FROM users WHERE id = $1",
    (row, meta) -> new User(
        row.get("id", Long.class),
        row.get("name", String.class),
        row.get("email", String.class)
    ),
    userId
).onSuccess(user -> log.info("Found: {}", user.name()));
```

## API Reference

### R2dbcOperations

```java
R2dbcOperations r2dbc = R2dbcOperations.r2dbcOperations(connectionFactory);
```

#### Query Methods

```java
// Single result (fails if 0 or >1 results)
Promise<User> user = r2dbc.queryOne(
    "SELECT * FROM users WHERE id = $1",
    this::mapUser,
    userId
);

// Optional result
Promise<Option<User>> user = r2dbc.queryOptional(
    "SELECT * FROM users WHERE email = $1",
    this::mapUser,
    email
);

// List of results
Promise<List<User>> users = r2dbc.queryList(
    "SELECT * FROM users WHERE active = $1",
    this::mapUser,
    true
);
```

#### Update Methods

```java
// Returns affected row count
Promise<Long> count = r2dbc.update(
    "UPDATE users SET active = $1 WHERE id = $2",
    false, userId
);
```

### Row Mapping

Map R2DBC rows using BiFunction:

```java
private User mapUser(Row row, RowMetadata meta) {
    return new User(
        row.get("id", Long.class),
        row.get("name", String.class),
        row.get("email", String.class),
        row.get("active", Boolean.class)
    );
}
```

### ReactiveOperations

Bridge between Reactive Streams Publisher and Promise:

```java
// Convert Publisher to Promise (single value)
Promise<T> result = ReactiveOperations.fromPublisher(publisher);

// Convert Publisher to Promise (first value as Option)
Promise<Option<T>> result = ReactiveOperations.firstFromPublisher(publisher);

// Convert Publisher to Promise (collect all values)
Promise<List<T>> result = ReactiveOperations.collectFromPublisher(publisher);
```

## Error Handling

All R2DBC errors are mapped to typed `R2dbcError` causes:

```java
r2dbc.queryOne("SELECT * FROM users WHERE id = $1", this::mapUser, userId)
    .onFailure(error -> {
        switch (error) {
            case R2dbcError.ConnectionFailed e ->
                log.error("Connection failed: {}", e.message());
            case R2dbcError.QueryFailed e ->
                log.error("Query failed: {} - SQL: {}", e.message(), e.sql());
            case R2dbcError.ConstraintViolation e ->
                log.warn("Constraint violated: {}", e.constraintName());
            case R2dbcError.Timeout e ->
                log.warn("Query timed out");
            case R2dbcError.NoResult e ->
                log.info("No result found");
            case R2dbcError.MultipleResults e ->
                log.error("Expected single result, got multiple");
            case R2dbcError.DatabaseFailure e ->
                log.error("Database error: {}", e.message());
        }
    });
```

## Complete Examples

### Repository Pattern

```java
public class UserRepository {
    private final R2dbcOperations r2dbc;

    public UserRepository(ConnectionFactory connectionFactory) {
        this.r2dbc = R2dbcOperations.r2dbcOperations(connectionFactory);
    }

    public Promise<Option<User>> findById(long id) {
        return r2dbc.queryOptional(
            "SELECT id, name, email, active FROM users WHERE id = $1",
            this::mapUser,
            id
        );
    }

    public Promise<List<User>> findByActive(boolean active) {
        return r2dbc.queryList(
            "SELECT id, name, email, active FROM users WHERE active = $1",
            this::mapUser,
            active
        );
    }

    public Promise<Long> save(User user) {
        return r2dbc.update(
            "INSERT INTO users (name, email, active) VALUES ($1, $2, $3)",
            user.name(), user.email(), user.active()
        );
    }

    public Promise<Long> deactivate(long id) {
        return r2dbc.update(
            "UPDATE users SET active = false WHERE id = $1",
            id
        );
    }

    private User mapUser(Row row, RowMetadata meta) {
        return new User(
            row.get("id", Long.class),
            row.get("name", String.class),
            row.get("email", String.class),
            row.get("active", Boolean.class)
        );
    }
}
```

### Parallel Queries

```java
public Promise<UserDashboard> loadDashboard(long userId) {
    return Promise.all(
        r2dbc.queryOptional(
            "SELECT * FROM users WHERE id = $1",
            this::mapUser,
            userId
        ),
        r2dbc.queryList(
            "SELECT * FROM orders WHERE user_id = $1 ORDER BY created_at DESC LIMIT 10",
            this::mapOrder,
            userId
        ),
        r2dbc.queryOne(
            "SELECT COUNT(*) as count FROM notifications WHERE user_id = $1 AND read = false",
            (row, _) -> row.get("count", Long.class),
            userId
        )
    ).flatMap((userOpt, orders, unreadCount) ->
        userOpt.toResult(UserError.NOT_FOUND)
            .map(user -> new UserDashboard(user, orders, unreadCount))
            .async()
    );
}
```

## Error Types

| Error Type | Description |
|------------|-------------|
| `ConnectionFailed` | Unable to establish database connection |
| `QueryFailed` | SQL query execution failed |
| `ConstraintViolation` | Database constraint violated |
| `Timeout` | Operation timed out |
| `NoResult` | Expected result but none returned |
| `MultipleResults` | Expected single result but got multiple |
| `DatabaseFailure` | General database error |

## Dependencies

- R2DBC SPI 1.0+
- R2DBC Driver (PostgreSQL, MySQL, H2, etc.)
- Reactive Streams 1.0+
- `pragmatica-lite-core`
