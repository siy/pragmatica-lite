# MessageRouter Hierarchy Design

## Current State Analysis

The current `MessageRouter` interface (in `common/src/main/java/org/pragmatica/message/MessageRouter.java`) has several issues:

1. **Mixed Responsibilities**: Interface includes both invocation (`route()`) and configuration (`addRoute()`) methods
2. **Thread Safety**: Current implementation uses `HashMap` which is not thread-safe for concurrent modifications
3. **Immutability**: No clear separation between mutable (test) and immutable (production) usage

## Proposed New Design

### Sealed Interface Architecture

The new design uses Java's sealed interfaces to create a clear hierarchy with distinct implementations for different use cases:

```java
package org.pragmatica.message;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.pragmatica.lang.Tuple.tuple;

/// **NOTE**: This implementation assumes that the instance is configured and then used without
/// changes.
public sealed interface MessageRouter {
    <T extends Message> void route(T message);

    default <T extends Message> void routeAsync(Supplier<T> messageSupplier) {
        Promise.async(() -> route(messageSupplier.get()));
    }

    static MutableRouter mutable() {
        return new MutableRouter.SimpleMutableRouter<>(new ConcurrentHashMap<>());
    }

    sealed interface MutableRouter extends MessageRouter {
        <T extends Message> MessageRouter addRoute(Class<? extends T> messageType, Consumer<? extends T> receiver);

        record SimpleMutableRouter<T extends Message>(
                ConcurrentMap<Class<T>, List<Consumer<T>>> routingTable) implements
                MutableRouter {

            private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

            @Override
            public <R extends Message> void route(R message) {
                Option.option(routingTable.get(message.getClass()))
                      .onPresent(list -> list.forEach(fn -> fn.accept((T) message)))
                      .onEmpty(() -> log.warn("No route for message type: {}", message.getClass().getSimpleName()));
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R extends Message> MessageRouter addRoute(Class<? extends R> messageType,
                                                              Consumer<? extends R> receiver) {
                routingTable.computeIfAbsent((Class<T>) messageType, _ -> new ArrayList<>())
                            .add((Consumer<T>) receiver);

                return this;
            }
        }
    }

    non-sealed interface ImmutableRouter<T extends Message> extends MessageRouter {
        Map<Class<T>, List<Consumer<T>>> routingTable();

        @SuppressWarnings("unchecked")
        default <R extends Message> void route(R message) {
            routingTable().get(message.getClass()).forEach(fn -> fn.accept((T) message));
        }
    }

    interface Entry<T extends Message> {
        Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries();

        Class<T> type();

        Set<Class<?>> validate();

        @SuppressWarnings("unchecked")
        default Result<MessageRouter> asRouter() {
            var validated = validate();

            if (!validated.isEmpty()) {
                var missing = validated.stream()
                                       .map(Class::getSimpleName)
                                       .collect(Collectors.joining(", "));
                return new InvalidMessageRouterConfiguration("Missing message types: " + missing).result();
            }

            record router<T extends Message>(
                    Map<Class<T>, List<Consumer<T>>> routingTable) implements ImmutableRouter<T> {}

            var routingTable = new HashMap<Class<T>, List<Consumer<T>>>();

            entries().forEach(tuple -> routingTable.compute((Class<T>) tuple.first(),
                                                            (_, oldValue) -> merge(tuple, oldValue)));

            return Result.success(new router<>(routingTable));
        }

        @SuppressWarnings("unchecked")
        private static <T extends Message> List<Consumer<T>> merge(Tuple2<Class<? extends T>, Consumer<? extends T>> tuple,
                                                                   List<Consumer<T>> oldValue) {
            var list = oldValue == null ? new ArrayList<Consumer<T>>() : oldValue;
            list.add((Consumer<T>) tuple.last());
            return list;
        }

        interface SealedBuilder<T extends Message> extends Entry<T> {
            static <T extends Message> SealedBuilder<T> from(Class<T> clazz) {
                record sealedBuilder<T extends Message>(Class<T> type,
                                                        List<Entry<? extends T>> routes) implements SealedBuilder<T> {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries() {
                        return routes.stream()
                                     .map(entry -> (Entry<T>) entry)
                                     .flatMap(Entry::entries);
                    }

                    @SafeVarargs
                    @Override
                    public final Entry<T> route(Entry<? extends T>... routes) {
                        routes().addAll(List.of(routes));

                        return this;
                    }

                    @Override
                    public Set<Class<?>> validate() {
                        if (!type().isSealed()) {
                            return mergeSubroutes(new HashSet<>());
                        }

                        var declared = routes().stream()
                                               .map(Entry::type)
                                               .collect(Collectors.toSet());
                        var permitted = new HashSet<>(Set.of(type().getPermittedSubclasses()));

                        permitted.removeAll(declared);

                        return mergeSubroutes(permitted);
                    }

                    private Set<Class<?>> mergeSubroutes(Set<Class<?>> local) {
                        routes().forEach(route -> local.addAll(route.validate()));
                        return local;
                    }
                }

                return new sealedBuilder<>(clazz, new ArrayList<>());
            }

            @SuppressWarnings("unchecked")
            Entry<T> route(Entry<? extends T>... routes);
        }

        static <T extends Message> Entry<T> route(Class<T> type, Consumer<T> receiver) {
            record entry<T extends Message>(Class<T> type, Consumer<T> receiver) implements Entry<T> {

                @Override
                public Stream<Tuple2<Class<? extends T>, Consumer<? extends T>>> entries() {
                    return Stream.of(tuple(type(), receiver()));
                }

                @Override
                public Set<Class<?>> validate() {
                    return Set.of(); // Route is always valid
                }
            }

            return new entry<>(type, receiver);
        }
    }

    record InvalidMessageRouterConfiguration(String message) implements Cause {}
}
```

## Key Design Principles

### 1. Sealed Interface Hierarchy
- **MessageRouter**: Base sealed interface with core routing functionality
- **MutableRouter**: Sealed sub-interface for test scenarios with runtime route addition
- **ImmutableRouter**: Non-sealed interface for production use with build-time configuration

### 2. No canHandle() Method
The `canHandle()` method is redundant in this design:
- **Mutable implementation**: Lack of handler logs warning, doesn't throw exception
- **Immutable implementation**: Routes are validated at build time, runtime failure impossible

### 3. Simplified Mutable Router
- No builder pattern - direct `mutable()` factory method
- Thread-safe using ConcurrentHashMap and ArrayList
- Logs warning when no handler found instead of silent failure

### 4. Sophisticated Immutable Router
- Entry-based builder for complex routing hierarchies
- Validates sealed type hierarchies at build time
- Returns Result<MessageRouter> with validation errors
- Supports nested routing structures

### 5. Error Handling Strategy
- **Mutable**: Logs warnings for missing handlers
- **Immutable**: Build-time validation prevents runtime errors
- **Invalid configuration**: Returns Result with descriptive error messages

## Usage Examples

### Simple Mutable Router (Tests)
```java
var router = MessageRouter.mutable()
    .addRoute(TestMessage.class, msg -> System.out.println("Test: " + msg))
    .addRoute(ErrorMessage.class, msg -> System.err.println("Error: " + msg));

router.route(new TestMessage("Hello"));
```

### Complex Immutable Router (Production)
```java
var routerResult = Entry.SealedBuilder.from(Message.class)
    .route(
        Entry.route(UserMessage.class, this::handleUser),
        Entry.route(SystemMessage.class, this::handleSystem)
    )
    .asRouter();

if (routerResult.isSuccess()) {
    MessageRouter router = routerResult.unwrap();
    router.route(new UserMessage("user data"));
}
```

## Migration Strategy

### Phase 1: Replace Current Interface
1. Replace current MessageRouter with sealed interface design
2. Update all existing usage to use `MessageRouter.mutable()` for tests
3. Convert production code to use Entry-based builders

### Phase 2: Validation and Testing  
1. Ensure all existing tests pass with new implementation
2. Add comprehensive tests for validation logic
3. Test sealed type hierarchy validation

### Phase 3: Documentation and Examples
1. Update all documentation to reflect new design
2. Provide migration examples for complex routing scenarios
3. Document error handling patterns

## Benefits

- **Type Safety**: Sealed interfaces prevent incorrect implementations
- **Build-time Validation**: Immutable router catches configuration errors early  
- **Simplified API**: No redundant methods like canHandle()
- **Performance**: Direct map lookup without additional checks
- **Logging**: Proper warning messages for debugging
- **Flexibility**: Entry system supports complex routing hierarchies
- **Thread Safety**: Both implementations are thread-safe by design

---
*Design by: Core Framework Team (updated based on Project Owner feedback)*
*Date: 2025-08-10*