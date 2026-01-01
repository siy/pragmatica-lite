# Messaging Module

Type-safe message routing with sealed interface validation.

## Features

- Type-safe message routing based on class hierarchy
- Compile-time validation for sealed interface coverage
- Mutable and immutable router implementations
- Async message routing support

## Usage

### Mutable Router

```java
import org.pragmatica.messaging.MessageRouter;
import org.pragmatica.messaging.Message;

// Define messages
record UserCreated(String userId) implements Message.Local {}
record OrderPlaced(String orderId) implements Message.Local {}

// Create and configure router
var router = MessageRouter.mutable();
router.addRoute(UserCreated.class, msg -> handleUserCreated(msg));
router.addRoute(OrderPlaced.class, msg -> handleOrderPlaced(msg));

// Route messages
router.route(new UserCreated("user-123"));
router.route(new OrderPlaced("order-456"));
```

### Immutable Router with Sealed Interface Validation

```java
import static org.pragmatica.messaging.MessageRouter.Entry.SealedBuilder.from;
import static org.pragmatica.messaging.MessageRouter.Entry.route;

// Define sealed hierarchy
sealed interface DomainEvent extends Message.Wired {
    record UserCreated(String id) implements DomainEvent {}
    record UserDeleted(String id) implements DomainEvent {}
}

// Build router with validation
var routes = from(DomainEvent.class)
    .route(
        route(DomainEvent.UserCreated.class, this::onUserCreated),
        route(DomainEvent.UserDeleted.class, this::onUserDeleted)
    );

// Validate all subtypes are covered
if (routes.validate().isEmpty()) {
    MessageRouter router = routes.asRouter().unsafe();
    router.route(new DomainEvent.UserCreated("user-1"));
}
```

## Message Types

- `Message.Local` - Messages that stay within a single process
- `Message.Wired` - Messages that can be serialized and sent over the network

## Annotations

`@MessageReceiver` - Documentation annotation for marking message handler methods.
