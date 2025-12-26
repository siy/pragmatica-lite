# JDBC Integration for Pragmatica Lite

Promise-based JDBC operations with typed error handling.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jdbc</artifactId>
    <version>0.9.0</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.jdbc.JdbcOperations;
import org.pragmatica.jdbc.JdbcTransactional;

// Create from DataSource
JdbcOperations jdbc = JdbcOperations.jdbcOperations(dataSource);

// Query single result
jdbc.queryOne("SELECT * FROM users WHERE id = ?", this::mapUser, userId)
    .onSuccess(user -> log.info("Found user: {}", user.name()))
    .onFailure(error -> log.error("Query failed: {}", error.message()));

// Query optional result
jdbc.queryOptional("SELECT * FROM users WHERE email = ?", this::mapUser, email)
    .onSuccess(optUser -> optUser
        .onPresent(user -> log.info("Found: {}", user))
        .onEmpty(() -> log.info("User not found")));
```

## API Reference

### JdbcOperations

```java
JdbcOperations jdbc = JdbcOperations.jdbcOperations(dataSource);
```

#### Query Methods

```java
// Single result (fails if 0 or >1 results)
Promise<User> user = jdbc.queryOne(
    "SELECT * FROM users WHERE id = ?",
    this::mapUser,
    userId
);

// Optional result (returns Option.none() if no results)
Promise<Option<User>> user = jdbc.queryOptional(
    "SELECT * FROM users WHERE email = ?",
    this::mapUser,
    email
);

// List of results
Promise<List<User>> users = jdbc.queryList(
    "SELECT * FROM users WHERE active = ?",
    this::mapUser,
    true
);
```

#### Update Methods

```java
// Single update
Promise<Integer> rowsAffected = jdbc.update(
    "UPDATE users SET name = ? WHERE id = ?",
    newName, userId
);

// Batch updates
List<Object[]> params = List.of(
    new Object[]{"Alice", "alice@example.com"},
    new Object[]{"Bob", "bob@example.com"}
);
Promise<int[]> results = jdbc.batch(
    "INSERT INTO users (name, email) VALUES (?, ?)",
    params
);
```

### Row Mapping

Map ResultSet rows to domain objects:

```java
private User mapUser(ResultSet rs) throws SQLException {
    return new User(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getBoolean("active")
    );
}
```

### JdbcTransactional

Execute operations within a transaction:

```java
JdbcTransactional.withTransaction(dataSource, JdbcError::fromException, conn -> {
    var jdbc = JdbcOperations.jdbcOperations(dataSource);

    return jdbc.update("INSERT INTO orders (user_id, total) VALUES (?, ?)", userId, total)
        .flatMap(_ -> jdbc.update("UPDATE users SET order_count = order_count + 1 WHERE id = ?", userId))
        .map(_ -> "Order created");
});
```

## Error Handling

All JDBC errors are mapped to typed `JdbcError` causes:

```java
jdbc.queryOne("SELECT * FROM users WHERE id = ?", this::mapUser, userId)
    .onFailure(error -> {
        switch (error) {
            case JdbcError.ConnectionFailed e ->
                log.error("Database connection failed: {}", e.message());
            case JdbcError.QueryFailed e ->
                log.error("Query failed: {} - SQL: {}", e.message(), e.sql());
            case JdbcError.ConstraintViolation e ->
                log.warn("Constraint violation: {}", e.constraintName());
            case JdbcError.Timeout e ->
                log.warn("Query timed out: {}", e.message());
            default ->
                log.error("Database error: {}", error.message());
        }
    });
```

## Complete Examples

### Repository Pattern

```java
public class UserRepository {
    private final JdbcOperations jdbc;

    public UserRepository(DataSource dataSource) {
        this.jdbc = JdbcOperations.jdbcOperations(dataSource);
    }

    public Promise<Option<User>> findById(long id) {
        return jdbc.queryOptional(
            "SELECT id, name, email, active FROM users WHERE id = ?",
            this::mapUser,
            id
        );
    }

    public Promise<List<User>> findByActive(boolean active) {
        return jdbc.queryList(
            "SELECT id, name, email, active FROM users WHERE active = ?",
            this::mapUser,
            active
        );
    }

    public Promise<User> save(User user) {
        return jdbc.update(
            "INSERT INTO users (name, email, active) VALUES (?, ?, ?)",
            user.name(), user.email(), user.active()
        ).map(_ -> user);
    }

    public Promise<Boolean> delete(long id) {
        return jdbc.update("DELETE FROM users WHERE id = ?", id)
            .map(count -> count > 0);
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getBoolean("active")
        );
    }
}
```

### Transaction with Multiple Operations

```java
public Promise<Order> createOrderWithItems(OrderRequest request) {
    return JdbcTransactional.withTransaction(dataSource, JdbcError::fromException, conn -> {
        var jdbc = JdbcOperations.jdbcOperations(dataSource);

        // Insert order
        return jdbc.queryOne(
            "INSERT INTO orders (user_id, total) VALUES (?, ?) RETURNING id",
            rs -> rs.getLong("id"),
            request.userId(), request.total()
        ).flatMap(orderId -> {
            // Insert order items
            var itemParams = request.items().stream()
                .map(item -> new Object[]{orderId, item.productId(), item.quantity()})
                .toList();

            return jdbc.batch(
                "INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, ?)",
                itemParams
            ).map(_ -> new Order(orderId, request.userId(), request.items()));
        });
    });
}
```

## Error Types

| Error Type | Description |
|------------|-------------|
| `ConnectionFailed` | Unable to obtain database connection |
| `QueryFailed` | SQL execution failed |
| `ConstraintViolation` | Unique/foreign key constraint violated |
| `Timeout` | Query execution timeout |
| `TransactionRollback` | Transaction was rolled back |
| `TransactionRequired` | Operation requires active transaction |
| `DatabaseFailure` | General database error |

## Dependencies

- JDBC Driver for your database
- `pragmatica-lite-core`
- Optional: HikariCP for connection pooling
