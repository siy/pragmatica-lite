# MessageRouter Hierarchy Design

## Current State Analysis

The current `MessageRouter` interface (in `common/src/main/java/org/pragmatica/message/MessageRouter.java`) has several issues:

1. **Mixed Responsibilities**: Interface includes both invocation (`route()`) and configuration (`addRoute()`) methods
2. **Thread Safety**: Current implementation uses `HashMap` which is not thread-safe for concurrent modifications
3. **Immutability**: No clear separation between mutable (test) and immutable (production) usage

## Proposed New Design

### Base Interface - MessageRouter
```java
package org.pragmatica.message;

import org.pragmatica.lang.annotations.StableAPI;

/**
 * Base interface for message routing - contains only invocation methods.
 * This interface provides the core routing functionality without configuration methods.
 */
@StableAPI
public interface MessageRouter {
    /**
     * Route a message to registered handlers.
     * @param message the message to route
     * @param <T> the message type
     */
    <T extends Message> void route(T message);
    
    /**
     * Route a message asynchronously.
     * @param messageSupplier supplier for the message
     * @param <T> the message type
     */
    default <T extends Message> void routeAsync(Supplier<T> messageSupplier) {
        Promise.async(() -> route(messageSupplier.get()));
    }
    
    /**
     * Check if this router can handle a specific message type.
     * @param messageType the message type to check
     * @return true if the router has handlers for this type
     */
    <T extends Message> boolean canHandle(Class<T> messageType);
}
```

### Mutable Implementation - For Tests
```java
package org.pragmatica.message;

import org.pragmatica.lang.annotations.TeamAPI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable message router implementation using ConcurrentHashMap for thread safety.
 * Intended for test scenarios where routes need to be added at runtime.
 */
@TeamAPI
public final class MutableMessageRouter implements MessageRouter {
    private final ConcurrentHashMap<Class<? extends Message>, List<Consumer<? extends Message>>> routes;
    
    private MutableMessageRouter() {
        this.routes = new ConcurrentHashMap<>();
    }
    
    /**
     * Add a route for a specific message type.
     * Thread-safe operation.
     */
    public <T extends Message> MutableMessageRouter addRoute(Class<T> messageType, Consumer<T> handler) {
        routes.computeIfAbsent(messageType, k -> new CopyOnWriteArrayList<>())
              .add((Consumer<Message>) handler);
        return this;
    }
    
    @Override
    public <T extends Message> void route(T message) {
        // Implementation using ConcurrentHashMap
    }
    
    /**
     * Simple builder for test scenarios.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final MutableMessageRouter router = new MutableMessageRouter();
        
        public <T extends Message> Builder addRoute(Class<T> messageType, Consumer<T> handler) {
            router.addRoute(messageType, handler);
            return this;
        }
        
        public MutableMessageRouter build() {
            return router;
        }
    }
}
```

### Immutable Implementation - For Production
```java
package org.pragmatica.message;

import org.pragmatica.lang.annotations.StableAPI;
import java.util.Map;

/**
 * Immutable message router implementation for production use.
 * Built using hierarchical builder pattern with from().route() syntax.
 */
@StableAPI 
public final class ImmutableMessageRouter implements MessageRouter {
    private final Map<Class<? extends Message>, List<Consumer<? extends Message>>> routes;
    
    private ImmutableMessageRouter(Map<Class<? extends Message>, List<Consumer<? extends Message>>> routes) {
        this.routes = Map.copyOf(routes); // Immutable copy
    }
    
    @Override
    public <T extends Message> void route(T message) {
        // Implementation using immutable map
    }
    
    /**
     * Start building with a message type - maintains existing from().route() pattern
     */
    public static <T extends Message> RouterBuilder<T> from(Class<T> messageType) {
        return new RouterBuilder<>(messageType);
    }
    
    // Hierarchical builder implementation...
}
```

## Migration Strategy

1. **Phase 1**: Implement new interfaces alongside existing code
2. **Phase 2**: Update all tests to use MutableMessageRouter.builder()  
3. **Phase 3**: Update production code to use ImmutableMessageRouter.from().route()
4. **Phase 4**: Remove RouterConfigurator and old MessageRouter.addRoute() method

## Benefits

- **Clear Separation**: Different implementations for different use cases
- **Thread Safety**: ConcurrentHashMap for mutable, immutability for production
- **API Stability**: @StableAPI for production interfaces
- **Backward Compatibility**: Existing from().route() pattern preserved

---
*Design by: Core Framework Team*
*Date: 2025-08-09*