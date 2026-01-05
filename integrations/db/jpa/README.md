# JPA Integration for Pragmatica Lite

Promise-based JPA EntityManager wrapper with typed error handling.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>jpa</artifactId>
    <version>0.9.7</version>
</dependency>
```

Requires Jakarta Persistence API:
```xml
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.1.0</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.jpa.JpaOperations;
import org.pragmatica.jpa.JpaError;
import org.pragmatica.jpa.Transactional;

// Create from EntityManager
JpaOperations jpa = JpaOperations.jpaOperations(entityManager);

// Query single result
var query = em.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class)
    .setParameter("id", userId);

jpa.querySingle(JpaError::fromException, query)
    .onSuccess(user -> log.info("Found: {}", user.getName()))
    .onFailure(error -> log.error("Query failed: {}", error.message()));
```

## API Reference

### JpaOperations

```java
JpaOperations jpa = JpaOperations.jpaOperations(entityManager);
```

#### Query Methods

```java
// Single result (fails if 0 or >1 results)
Promise<User> user = jpa.querySingle(JpaError::fromException, typedQuery);

// Optional result (throws NonUniqueResultException if >1 result)
Promise<Option<User>> user = jpa.queryOptional(JpaError::fromException, typedQuery);

// List of results
Promise<List<User>> users = jpa.queryList(JpaError::fromException, typedQuery);

// Find by ID
Promise<Option<User>> user = jpa.findById(JpaError::fromException, User.class, userId);
```

#### Entity Operations

```java
// Persist new entity
Promise<User> persisted = jpa.persist(JpaError::fromException, newUser);

// Merge entity state
Promise<User> merged = jpa.merge(JpaError::fromException, detachedUser);

// Remove entity
Promise<Unit> removed = jpa.remove(JpaError::fromException, user);

// Refresh from database
Promise<User> refreshed = jpa.refresh(JpaError::fromException, user);

// Flush pending changes
Promise<Unit> flushed = jpa.flush(JpaError::fromException);
```

#### Update Queries

```java
var query = em.createQuery("UPDATE User u SET u.active = false WHERE u.lastLogin < :date")
    .setParameter("date", cutoffDate);

Promise<Integer> count = jpa.executeUpdate(JpaError::fromException, query);
```

### Transactional

Execute operations within a managed transaction:

```java
Transactional.withTransaction(entityManager, JpaError::fromException, em -> {
    var jpa = JpaOperations.jpaOperations(em);

    return jpa.persist(JpaError::fromException, newOrder)
        .flatMap(_ -> jpa.executeUpdate(JpaError::fromException, updateInventoryQuery));
});
```

## Error Handling

All JPA errors are mapped to typed `JpaError` causes:

```java
jpa.persist(JpaError::fromException, entity)
    .onFailure(error -> {
        switch (error) {
            case JpaError.EntityNotFound e ->
                log.warn("Entity not found: {}", e.entityType());
            case JpaError.OptimisticLock e ->
                log.warn("Concurrent modification of {}", e.entityType());
            case JpaError.PessimisticLock e ->
                log.error("Could not acquire lock on {}", e.entityType());
            case JpaError.ConstraintViolation e ->
                log.warn("Constraint violated: {}", e.constraintName());
            case JpaError.EntityExists e ->
                log.warn("Entity already exists: {}", e.entityType());
            case JpaError.TransactionRequired e ->
                log.error("Operation requires transaction");
            case JpaError.QueryTimeout e ->
                log.warn("Query timed out: {}", e.message());
            case JpaError.DatabaseFailure e ->
                log.error("Database error: {}", e.message());
        }
    });
```

## Complete Examples

### Repository Pattern

```java
public class UserRepository {
    private final EntityManager em;
    private final JpaOperations jpa;

    public UserRepository(EntityManager em) {
        this.em = em;
        this.jpa = JpaOperations.jpaOperations(em);
    }

    public Promise<Option<User>> findById(Long id) {
        return jpa.findById(JpaError::fromException, User.class, id);
    }

    public Promise<List<User>> findByRole(Role role) {
        var query = em.createQuery(
            "SELECT u FROM User u WHERE u.role = :role ORDER BY u.name",
            User.class
        ).setParameter("role", role);

        return jpa.queryList(JpaError::fromException, query);
    }

    public Promise<Option<User>> findByEmail(String email) {
        var query = em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email",
            User.class
        ).setParameter("email", email);

        return jpa.queryOptional(JpaError::fromException, query);
    }

    public Promise<User> save(User user) {
        if (user.getId() == null) {
            return jpa.persist(JpaError::fromException, user);
        }
        return jpa.merge(JpaError::fromException, user);
    }

    public Promise<Unit> delete(User user) {
        return jpa.remove(JpaError::fromException, user);
    }
}
```

### Transactional Use Case

```java
public class TransferMoney implements UseCase<TransferRequest, TransferResult> {
    private final EntityManager em;

    public Promise<TransferResult> execute(TransferRequest request) {
        return Transactional.withTransaction(em, JpaError::fromException, txEm -> {
            var jpa = JpaOperations.jpaOperations(txEm);

            return jpa.findById(JpaError::fromException, Account.class, request.fromAccountId())
                .flatMap(fromOpt -> fromOpt.toResult(AccountError.NOT_FOUND).async())
                .flatMap(from -> {
                    if (from.getBalance().compareTo(request.amount()) < 0) {
                        return AccountError.INSUFFICIENT_FUNDS.<TransferResult>result().async();
                    }

                    return jpa.findById(JpaError::fromException, Account.class, request.toAccountId())
                        .flatMap(toOpt -> toOpt.toResult(AccountError.NOT_FOUND).async())
                        .flatMap(to -> {
                            from.withdraw(request.amount());
                            to.deposit(request.amount());

                            return jpa.merge(JpaError::fromException, from)
                                .flatMap(_ -> jpa.merge(JpaError::fromException, to))
                                .map(_ -> new TransferResult(from.getId(), to.getId(), request.amount()));
                        });
                });
        });
    }
}
```

## Error Types

| Error Type | Description |
|------------|-------------|
| `EntityNotFound` | Entity with given ID not found |
| `OptimisticLock` | Concurrent modification detected |
| `PessimisticLock` | Could not acquire database lock |
| `ConstraintViolation` | Unique/foreign key constraint violated |
| `TransactionRequired` | Operation requires active transaction |
| `EntityExists` | Entity with same ID already exists |
| `QueryTimeout` | Query execution timeout |
| `DatabaseFailure` | General database/JPA error |

## Dependencies

- Jakarta Persistence API 3.1+
- JPA Provider (Hibernate, EclipseLink, etc.)
- `pragmatica-lite-core`
