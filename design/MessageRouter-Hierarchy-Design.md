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
 * 
 * <p><strong>Dispatch Semantics:</strong></p>
 * <ul>
 *   <li>Exact-type matching: handlers are matched based on exact message class</li>
 *   <li>Multiple handlers: all registered handlers for a type are invoked</li>
 *   <li>Execution order: handlers execute in registration order</li>
 *   <li>Exception handling: first handler exception terminates processing</li>
 *   <li>No handlers: silent no-op (no exception thrown)</li>
 * </ul>
 */
@StableAPI
public interface MessageRouter {
    /**
     * Route a message to all registered handlers for its exact type.
     * 
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>All handlers for the message type are invoked in registration order</li>
     *   <li>If any handler throws an exception, processing stops and exception propagates</li>
     *   <li>If no handlers are registered, this is a silent no-op</li>
     * </ul>
     * 
     * @param message the message to route (must not be null)
     * @param <T> the message type
     * @throws NullPointerException if message is null
     * @throws RuntimeException if any handler throws an exception
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
     * 
     * @param messageType the message type to check (must not be null)
     * @return true if the router has one or more handlers for this exact type
     * @throws NullPointerException if messageType is null
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
 * <p><strong>⚠️ TEST USE ONLY:</strong> This implementation is intended exclusively for test scenarios 
 * where routes need to be added at runtime. Do not use in production code.</p>
 * 
 * <p><strong>Threading Model:</strong></p>
 * <ul>
 *   <li>Thread-safe for concurrent route additions and message routing</li>
 *   <li>Uses ConcurrentHashMap and CopyOnWriteArrayList for thread safety</li>
 *   <li>Route registration may block briefly during list copying</li>
 * </ul>
 */
@TeamAPI
final class MutableMessageRouter implements MessageRouter { // package-private - test use only
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
 * 
 * <p><strong>Threading Model:</strong></p>
 * <ul>
 *   <li>Completely thread-safe - all internal state is immutable</li>
 *   <li>Zero synchronization overhead during message routing</li>
 *   <li>Safe to share across multiple threads and components</li>
 * </ul>
 * 
 * <p><strong>Performance Characteristics:</strong></p>
 * <ul>
 *   <li>O(1) handler lookup using HashMap</li>
 *   <li>O(n) handler execution where n = number of handlers for message type</li>
 * </ul>
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

### Release 1.0-rc1 (Immediate)
1. **Phase 1**: Implement new interfaces alongside existing code
   - Add new MessageRouter interface with routing-only methods
   - Implement MutableMessageRouter (package-private, tests only)
   - Implement ImmutableMessageRouter with hierarchical builder
   - **Deprecation**: Mark `RouterConfigurator` with `@Deprecated` and `@ScheduledForRemoval(inVersion = "1.0")`
   - **Deprecation**: Mark existing `MessageRouter.addRoute()` with `@Deprecated` and `@ScheduledForRemoval(inVersion = "1.0")`

### Release 1.0-rc2
2. **Phase 2**: Update all test code
   - Migrate all test code to use `MutableMessageRouter.builder()` 
   - Provide migration guide and examples
   - **Compatibility**: Create `LegacyRouterAdapter` that wraps new implementations

### Release 1.0-rc3
3. **Phase 3**: Update production code
   - Migrate all production code to use `ImmutableMessageRouter.from().route()`
   - Update documentation and examples
   - **Validation**: Compile-time errors for deprecated API usage

### Release 1.0.0 (Final)
4. **Phase 4**: Remove deprecated code
   - Remove `RouterConfigurator` completely
   - Remove old `MessageRouter.addRoute()` method
   - Remove `LegacyRouterAdapter`
   - Clean up all deprecation warnings

### Migration Compatibility Adapter
```java
/**
 * Temporary adapter for migrating from old RouterConfigurator pattern.
 * @deprecated Use ImmutableMessageRouter.from().route() directly
 */
@Deprecated
@ScheduledForRemoval(inVersion = "1.0")
public final class LegacyRouterAdapter {
    public static MessageRouter fromConfigurator(RouterConfigurator config) {
        // Convert old configuration to new ImmutableMessageRouter
    }
}
```

## Benefits

- **Clear Separation**: Different implementations for different use cases
- **Thread Safety**: ConcurrentHashMap for mutable, immutability for production  
- **API Stability**: @StableAPI for production interfaces
- **Backward Compatibility**: Existing from().route() pattern preserved
- **Explicit Semantics**: Clear documentation of dispatch behavior, handler ordering, and exception handling
- **Test Isolation**: Package-private MutableMessageRouter prevents accidental production usage
- **Migration Support**: Structured release plan with deprecation timeline and compatibility adapters

---
*Design by: Core Framework Team*
*Date: 2025-08-09*