# JOOQ R2DBC Integration for Pragmatica Lite

Promise-based JOOQ with R2DBC for type-safe reactive database access.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jooq-r2dbc</artifactId>
    <version>0.11.0</version>
</dependency>
```

Requires JOOQ and R2DBC:
```xml
<dependency>
    <groupId>org.jooq</groupId>
    <artifactId>jooq</artifactId>
    <version>3.19.0</version>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.jooq.r2dbc.JooqR2dbcOperations;
import org.jooq.SQLDialect;

var connectionFactory = ConnectionFactories.get(
    "r2dbc:postgresql://localhost:5432/mydb"
);

JooqR2dbcOperations jooq = JooqR2dbcOperations.jooqR2dbcOperations(connectionFactory, SQLDialect.POSTGRES);

// Type-safe query
jooq.fetchOptional(
    jooq.dsl().selectFrom(USERS).where(USERS.ID.eq(userId))
).onSuccess(optUser ->
    optUser.onPresent(record -> log.info("Found: {}", record.getName()))
);
```

## API Reference

### JooqR2dbcOperations

```java
// With explicit dialect
JooqR2dbcOperations jooq = JooqR2dbcOperations.jooqR2dbcOperations(connectionFactory, SQLDialect.POSTGRES);

// With default dialect
JooqR2dbcOperations jooq = JooqR2dbcOperations.jooqR2dbcOperations(connectionFactory);
```

#### Query Methods

```java
// Single result (fails if 0 or >1 results)
Promise<UserRecord> user = jooq.fetchOne(
    jooq.dsl().selectFrom(USERS).where(USERS.ID.eq(userId))
);

// Optional result
Promise<Option<UserRecord>> user = jooq.fetchOptional(
    jooq.dsl().selectFrom(USERS).where(USERS.EMAIL.eq(email))
);

// List of results
Promise<List<UserRecord>> users = jooq.fetch(
    jooq.dsl().selectFrom(USERS).where(USERS.ACTIVE.isTrue())
);
```

#### Execute Methods

```java
// INSERT, UPDATE, DELETE - returns affected row count
Promise<Integer> count = jooq.execute(
    jooq.dsl().update(USERS)
        .set(USERS.ACTIVE, false)
        .where(USERS.LAST_LOGIN.lt(cutoffDate))
);
```

#### DSL Context

Access the DSLContext for building queries:

```java
DSLContext dsl = jooq.dsl();

var query = dsl.select(USERS.NAME, ORDERS.TOTAL)
    .from(USERS)
    .join(ORDERS).on(ORDERS.USER_ID.eq(USERS.ID))
    .where(ORDERS.STATUS.eq("COMPLETED"));

jooq.fetch(query).onSuccess(records -> /* ... */);
```

### JooqR2dbcTransactional

Execute operations within a transaction:

```java
JooqR2dbcTransactional.withTransaction(connectionFactory, SQLDialect.POSTGRES, dsl -> {
    var ops = JooqR2dbcOperations.jooqR2dbcOperations(dsl);

    return ops.execute(dsl.insertInto(ORDERS)
            .set(ORDERS.USER_ID, userId)
            .set(ORDERS.TOTAL, total))
        .flatMap(_ -> ops.execute(dsl.update(USERS)
            .set(USERS.ORDER_COUNT, USERS.ORDER_COUNT.plus(1))
            .where(USERS.ID.eq(userId))));
});
```

## Complete Examples

### Type-Safe Repository

```java
public class UserRepository {
    private final JooqR2dbcOperations jooq;

    public UserRepository(ConnectionFactory connectionFactory) {
        this.jooq = JooqR2dbcOperations.jooqR2dbcOperations(connectionFactory, SQLDialect.POSTGRES);
    }

    public Promise<Option<User>> findById(Long id) {
        return jooq.fetchOptional(
            jooq.dsl().selectFrom(USERS).where(USERS.ID.eq(id))
        ).map(opt -> opt.map(this::toDomain));
    }

    public Promise<List<User>> findByRole(Role role) {
        return jooq.fetch(
            jooq.dsl().selectFrom(USERS)
                .where(USERS.ROLE.eq(role.name()))
                .orderBy(USERS.NAME)
        ).map(records -> records.stream().map(this::toDomain).toList());
    }

    public Promise<Option<User>> findByEmail(String email) {
        return jooq.fetchOptional(
            jooq.dsl().selectFrom(USERS).where(USERS.EMAIL.eq(email))
        ).map(opt -> opt.map(this::toDomain));
    }

    public Promise<User> save(User user) {
        if (user.id() == null) {
            return jooq.fetchOne(
                jooq.dsl().insertInto(USERS)
                    .set(USERS.NAME, user.name())
                    .set(USERS.EMAIL, user.email())
                    .set(USERS.ROLE, user.role().name())
                    .returning()
            ).map(this::toDomain);
        }
        return jooq.execute(
            jooq.dsl().update(USERS)
                .set(USERS.NAME, user.name())
                .set(USERS.EMAIL, user.email())
                .set(USERS.ROLE, user.role().name())
                .where(USERS.ID.eq(user.id()))
        ).map(_ -> user);
    }

    public Promise<Boolean> delete(Long id) {
        return jooq.execute(
            jooq.dsl().deleteFrom(USERS).where(USERS.ID.eq(id))
        ).map(count -> count > 0);
    }

    private User toDomain(UserRecord record) {
        return new User(
            record.getId(),
            record.getName(),
            record.getEmail(),
            Role.valueOf(record.getRole())
        );
    }
}
```

### Complex Joins

```java
public Promise<List<OrderSummary>> getOrderSummaries(Long userId) {
    return jooq.fetch(
        jooq.dsl()
            .select(
                ORDERS.ID,
                ORDERS.CREATED_AT,
                ORDERS.STATUS,
                DSL.sum(ORDER_ITEMS.QUANTITY.multiply(ORDER_ITEMS.PRICE)).as("total")
            )
            .from(ORDERS)
            .join(ORDER_ITEMS).on(ORDER_ITEMS.ORDER_ID.eq(ORDERS.ID))
            .where(ORDERS.USER_ID.eq(userId))
            .groupBy(ORDERS.ID, ORDERS.CREATED_AT, ORDERS.STATUS)
            .orderBy(ORDERS.CREATED_AT.desc())
    ).map(records -> records.stream()
        .map(r -> new OrderSummary(
            r.get(ORDERS.ID),
            r.get(ORDERS.CREATED_AT),
            OrderStatus.valueOf(r.get(ORDERS.STATUS)),
            r.get("total", BigDecimal.class)
        ))
        .toList()
    );
}
```

### Transactional Order Creation

```java
public Promise<Order> createOrder(CreateOrderRequest request) {
    return JooqR2dbcTransactional.withTransaction(
        connectionFactory,
        SQLDialect.POSTGRES,
        dsl -> {
            var ops = JooqR2dbcOperations.jooqR2dbcOperations(dsl);

            // Insert order
            return ops.fetchOne(
                dsl.insertInto(ORDERS)
                    .set(ORDERS.USER_ID, request.userId())
                    .set(ORDERS.STATUS, "PENDING")
                    .returning()
            ).flatMap(order -> {
                // Insert order items
                var inserts = request.items().stream()
                    .map(item -> dsl.insertInto(ORDER_ITEMS)
                        .set(ORDER_ITEMS.ORDER_ID, order.getId())
                        .set(ORDER_ITEMS.PRODUCT_ID, item.productId())
                        .set(ORDER_ITEMS.QUANTITY, item.quantity())
                        .set(ORDER_ITEMS.PRICE, item.price()))
                    .toList();

                return Promise.allOf(inserts.stream().map(ops::execute).toList())
                    .map(_ -> toDomain(order, request.items()));
            });
        }
    );
}
```

## Error Handling

Uses `R2dbcError` from the R2DBC module:

```java
jooq.fetchOne(query)
    .onFailure(error -> {
        switch (error) {
            case R2dbcError.NoResult _ ->
                log.info("Record not found");
            case R2dbcError.MultipleResults _ ->
                log.error("Expected single result");
            case R2dbcError.ConstraintViolation e ->
                log.warn("Constraint violated: {}", e.constraintName());
            default ->
                log.error("Database error: {}", error.message());
        }
    });
```

## Dependencies

- JOOQ 3.19+
- R2DBC SPI 1.0+
- R2DBC Driver
- `pragmatica-lite-r2dbc`
- `pragmatica-lite-core`
