# Micrometer Integration for Pragmatica Lite

Aspect decorators for adding Micrometer metrics to Promise-returning functions.

## Installation

```xml
<dependency>
    <groupId>org.pragmatica-lite</groupId>
    <artifactId>micrometer</artifactId>
    <version>0.9.10</version>
</dependency>
```

Requires Micrometer Core:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.12.0</version>
</dependency>
```

## Quick Start

```java
import org.pragmatica.metrics.PromiseMetrics;
import io.micrometer.core.instrument.MeterRegistry;

// Create metrics wrapper
var metrics = PromiseMetrics.timer("order.process")
    .registry(meterRegistry)
    .tags("service", "orders")
    .build();

// Wrap a Promise-returning function
Fn1<Promise<Order>, OrderRequest> processOrder = metrics.around(orderService::process);

// Use the wrapped function - metrics recorded automatically
processOrder.apply(request)
    .onSuccess(order -> log.info("Order processed: {}", order.id()));
```

## Metrics Types

### Timer Metrics

Records both duration and count, with separate timers for success/failure:

```java
var metrics = PromiseMetrics.timer("api.request")
    .registry(meterRegistry)
    .tags("endpoint", "/users", "method", "GET")
    .build();

// Creates metrics:
// - api.request.success (timer) - duration + count of successes
// - api.request.failure (timer) - duration + count of failures
```

### Counter Metrics

Records only success/failure counts (no duration):

```java
var metrics = PromiseMetrics.counter("events.processed")
    .registry(meterRegistry)
    .tags("type", "order")
    .build();

// Creates metrics:
// - events.processed.success (counter)
// - events.processed.failure (counter)
```

### Combined Metrics

Records a single timer for all operations plus separate success/failure counters:

```java
var metrics = PromiseMetrics.combined("db.query")
    .registry(meterRegistry)
    .tags("table", "users")
    .build();

// Creates metrics:
// - db.query (timer) - duration + total count
// - db.query.success (counter)
// - db.query.failure (counter)
```

## API Reference

### Builder Pattern

```java
PromiseMetrics metrics = PromiseMetrics.timer("metric.name")
    .registry(meterRegistry)           // Required: Micrometer registry
    .tags("key1", "value1")            // Optional: add tags
    .tags("key2", "value2")            // Can chain multiple
    .build();
```

### Wrapping Functions

```java
// Wrap Fn1 (function with input)
Fn1<Promise<Output>, Input> wrapped = metrics.around(originalFunction);

// Wrap Supplier (no input)
Supplier<Promise<Output>> wrapped = metrics.around(originalSupplier);
```

## Complete Examples

### Repository Layer Metrics

```java
public class UserRepository {
    private final JdbcOperations jdbc;
    private final PromiseMetrics findMetrics;
    private final PromiseMetrics saveMetrics;

    public UserRepository(JdbcOperations jdbc, MeterRegistry registry) {
        this.jdbc = jdbc;
        this.findMetrics = PromiseMetrics.timer("repository.user.find")
            .registry(registry)
            .build();
        this.saveMetrics = PromiseMetrics.timer("repository.user.save")
            .registry(registry)
            .build();
    }

    public Promise<Option<User>> findById(UserId id) {
        return findMetrics.around(() ->
            jdbc.queryOptional("SELECT * FROM users WHERE id = ?",
                this::mapUser, id.value())
        ).get();
    }

    public Promise<User> save(User user) {
        return saveMetrics.around(() ->
            jdbc.update("INSERT INTO users (name, email) VALUES (?, ?)",
                user.name(), user.email())
                .map(_ -> user)
        ).get();
    }
}
```

### API Endpoint Metrics

```java
public class OrderController {
    private final ProcessOrder processOrder;
    private final PromiseMetrics metrics;

    public OrderController(ProcessOrder processOrder, MeterRegistry registry) {
        this.processOrder = processOrder;
        this.metrics = PromiseMetrics.combined("api.orders.create")
            .registry(registry)
            .tags("version", "v1")
            .build();
    }

    public Promise<OrderResponse> createOrder(OrderRequest request) {
        return metrics.around(processOrder::execute).apply(request)
            .map(OrderResponse::from);
    }
}
```

### Multiple Tagged Instances

```java
public class MetricsFactory {
    private final MeterRegistry registry;

    public PromiseMetrics httpClientMetrics(String service, String endpoint) {
        return PromiseMetrics.timer("http.client.request")
            .registry(registry)
            .tags("service", service, "endpoint", endpoint)
            .build();
    }

    public PromiseMetrics databaseMetrics(String table, String operation) {
        return PromiseMetrics.timer("database.operation")
            .registry(registry)
            .tags("table", table, "operation", operation)
            .build();
    }
}
```

## Result and Option Metrics

For synchronous Result/Option operations, use dedicated wrappers:

```java
// ResultMetrics - for Result<T> returning functions
var resultMetrics = ResultMetrics.counter("validation")
    .registry(registry)
    .build();

Fn1<Result<ValidRequest>, RawRequest> validate =
    resultMetrics.around(validator::validate);

// OptionMetrics - for Option<T> returning functions
var optionMetrics = OptionMetrics.counter("cache.lookup")
    .registry(registry)
    .build();

Fn1<Option<User>, UserId> cacheLookup =
    optionMetrics.around(cache::get);
```

## Dependencies

- Micrometer Core 1.12+
- `pragmatica-lite-core`
