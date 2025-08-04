# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pragmatica Lite is a Java 21 functional programming framework built around three core monads: `Option<T>`, `Result<T>`, and `Promise<T>`. The project emphasizes null-safety, exception-free error handling, and distributed system capabilities through the Rabia consensus algorithm.

## Essential Commands

### Build and Test
```bash
# Build entire project
mvn clean compile

# Run all tests (excludes Infinite, Slow, Benchmark groups)
mvn test

# Run tests for specific module
mvn test -pl core
mvn test -pl cluster

# Run specific test class
mvn test -Dtest=OptionTest -pl core
mvn test -Dtest=RabiaEngineTest -pl cluster

# Install to local repository
mvn install

# Check for dependency updates
./script/check-dependency-updates.sh

# Update Maven wrapper
./script/update-wrapper.sh
```

### Development
```bash
# Enable preview features compilation
mvn compile -Djava.enable-preview

# Skip excluded test groups and run all tests
mvn test -Dgroups="!Infinite,!Slow,!Benchmark"
```

## Architecture Overview

### Core Module Structure
The framework is built around three foundational monads that provide consistent APIs:

- **`Option<T>`**: Handles potentially missing values (Some/None pattern)  
- **`Result<T>`**: Manages operations that may fail (Success/Failure pattern)
- **`Promise<T>`**: Represents eventually available values (asynchronous Result)

All monads support:
- Transformation operations (`map`, `flatMap`, `fold`)
- Type-safe predicates (`all`, `any`) supporting up to 9-tuple combinations
- Seamless inter-conversion between monads
- Pattern matching via sealed interfaces

### Distributed Systems Architecture

**Cluster Module** implements a complete distributed consensus system:

1. **Rabia Consensus Engine** (`cluster/consensus/rabia/`):
   - `RabiaEngine`: Core consensus protocol implementation
   - `RabiaEngineIO`: I/O operations and message handling
   - `RabiaPersistence`: State persistence (currently in-memory)
   - Batch-based command processing with correlation IDs

2. **Message Router** (`common/message/`):
   - Targeted message delivery (not broadcast)
   - Decouples components and simplifies interactions
   - `MessageRouter`: Core routing logic
   - `RouterConfigurator`: Setup and configuration

3. **State Machine Interface** (`cluster/state/`):
   - Pluggable application logic via `StateMachine` interface
   - `KVStore` implementation with structured keys
   - Deterministic command processing across nodes

4. **Networking Layer** (`cluster/net/netty/`):
   - Netty-based cluster communication
   - Binary serialization (Kryo, Fury support)
   - Connection management and failure detection

5. **Leader Management** (`cluster/leader/`):
   - Deterministic leader selection for cluster-wide operations
   - Leader change notifications
   - Works with leaderless Rabia consensus

### Key Architectural Patterns

**Monadic Composition**: All three core monads use identical patterns for chaining operations. When working with any monad, prefer method chaining over imperative style.

**Error Propagation**: Use `Result<T>` for all operations that may fail. Never throw exceptions in business logic - convert them using `Result.lift()` methods.

**Asynchronous Processing**: `Promise<T>` provides two distinct models:
- **Dependent actions**: Sequential transformations (`map`, `flatMap`) 
- **Independent actions**: Parallel execution (`onResult`, `onSuccess`)

**Message-Driven Architecture**: The cluster module uses message routing rather than direct method calls, enabling loose coupling and easier testing.

## Module Interdependencies

```
examples ─┐
cluster ──┼─→ common ─→ core
net-core ─┘
```

- **core**: Foundation monads, no external dependencies
- **common**: Shared utilities, depends only on core  
- **cluster**: Consensus implementation, depends on common + net-core
- **net-core**: Networking layer, depends on core
- **examples**: Usage demonstrations, depends on core

## Core Patterns

The framework implements seven key patterns for Pragmatic Functional Java:

### 1. **Sequencer Pattern**
Sequential processing pipeline where each step depends on the previous one. Error handling is built into the pipeline using `flatMap` chains.

```java
Result<FinalOutput> process(Input input) {
    return Result.success(input)
        .flatMap(this::validateInput)      // Step 1
        .flatMap(this::enrichWithExternalData)  // Step 2
        .flatMap(this::transform)         // Step 3
        .map(this::formatOutput);         // Step 4
}
```

### 2. **Fork-Join (Fan-Out-Fan-In) Pattern**
Encodes independence of operations, enabling concurrent execution. Uses `Promise.all()` or `Result.all()` to join results.

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.success(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            var p3 = fetchDataC(validated);
            
            return Promise.all(p1, p2, p3)
                .flatMap((dataA, dataB, dataC) -> 
                    combine(dataA, dataB, dataC));
        });
}
```

### 3. **Aspect Pattern**
Functional approach to cross-cutting concerns using higher-order functions to wrap operations with pre/post-processing.

```java
Function<Promise<T>, Promise<T>> loggingAspect(String operationName) {
    return operation -> Promise.success()
        .onResult(_ -> log.info("Starting: {}", operationName))
        .flatMap(_ -> operation)
        .onResult(result -> log.info("Completed: {}", operationName));
}
```

### 4. **Leaf Pattern** 
Terminal operations that bridge between functional and imperative code. Use `Result.lift()` and `Promise.lift()` to wrap external dependencies.

```java
Result<UserProfile> fetchUser(String userId) {
    return Result.lift(() -> userService.getUserById(userId))
        .filter(profile -> profile != null, 
                () -> Causes.of("User not found: " + userId));
}
```

### 5. **Condition Pattern**
Expression-based branching using ternary operators and switch expressions, maintaining single level of abstraction.

```java
Result<ShippingOption> determineShipping(Order order) {
    return Result.success(
        switch(order.getRegion()) {
            case DOMESTIC -> determineDomesticShipping(order);
            case INTERNATIONAL -> determineInternationalShipping(order);
        }
    );
}
```

### 6. **Iteration Pattern**
Declarative processing of collections, recursive operations, and repetitive tasks using functional transformations.

```java
Result<ProcessedData> processItems(List<Item> items) {
    return Result.success(items)
        .map(this::validateItems)
        .flatMap(this::transformItems)
        .map(this::aggregateResults);
}
```

## Development Conventions

### Documentation Standards
**Use Java 23 Markdown Documentation Comments (JEP 467)**: All documentation should use the new `///` Markdown comment syntax instead of traditional `/** */` JavaDoc:
- Use `/// Comment text` instead of `/** Comment text */`
- Use `**bold**` instead of `<b>bold</b>`
- Use `*italic*` instead of `<i>italic</i>`
- Use `- item` for bullet lists instead of `<li>item</li>`
- Use `1. item` for numbered lists
- Use backticks for code: \`code\` instead of `<code>code</code>`
- Use fenced code blocks with language specified: ```java instead of `<pre><code>`
- Parameter documentation: `- **param**: description` instead of `@param param description`
- Return documentation: `- **Returns**: description` instead of `@return description`

### Single Level of Abstraction Principle
Each method should operate at exactly one level of abstraction. Extract complex operations into separate methods rather than nesting multiple patterns.

```java
// Good - single level of abstraction
Result<Order> processOrder(Request request) {
    return validateRequest(request)
        .flatMap(this::createOrder)
        .flatMap(this::saveOrder);
}

// Bad - multiple levels mixed
Result<Order> processOrder(Request request) {
    return validateRequest(request)
        .flatMap(validRequest -> {
            // This lambda contains business logic - extract to method
            if (validRequest.isPriority()) {
                return priorityOrderService.create(validRequest);
            }
            return standardOrderService.create(validRequest);
        });
}
```

### Monad Usage Patterns
```java
// Use lift() to wrap external operations
Result<Data> fetchData(String id) {
    return Result.lift(() -> externalApi.getData(id))
        .mapError(e -> Causes.wrap(e, "Failed to fetch: " + id));
}

// Chain transformations with map/flatMap
return Option.option(getValue())
    .map(String::toUpperCase)
    .flatMap(this::processValue)
    .or("default");

// Promise.all() parameters are passed directly to flatMap
return Promise.all(p1, p2, p3)
    .flatMap((v1, v2, v3) -> combine(v1, v2, v3));
```

### Testing Approach
- Integration tests in `cluster` module test full consensus scenarios
- Core monad tests focus on transformation correctness
- Use `@Deprecated` `.unwrap()` methods only in test code
- Exclude long-running tests with `@Tag("Slow")` or similar

### Serialization
- Binary serialization using Kryo or Fury for cluster communication
- Implement custom serializers in `net-core/serialization/binary/`
- Register classes in `ClassRegistrator` implementations

## Core Philosophy: Pragmatic Functional Java

This framework emphasizes **pragmatic** functional programming - applying functional patterns selectively where they add value rather than pursuing theoretical purity:

- **Limited Scope**: Focus on three core monads (`Option`, `Result`, `Promise`) rather than full functional programming
- **Practical Approach**: Works with existing Java libraries and doesn't require immutability everywhere  
- **Error Transparency**: Explicit error handling through `Result<T>` rather than exceptions in business logic
- **Incremental Adoption**: Can be introduced gradually to existing Java codebases
- **Developer Friendly**: Familiar Java syntax without requiring paradigm shifts

## Key Implementation Notes

### Error Handling Strategy
- Use `Result<T>` for all operations that may fail
- Convert exceptions to `Result` using `lift()` methods at system boundaries (Leaf pattern)
- Prefer `trace()` method for debugging error propagation paths
- Never throw exceptions in business logic - always return `Result<T>`

### Async Processing with Promise<T>
- **Dependent Actions**: Sequential operations using `map`, `flatMap` - executed in order
- **Independent Actions**: Parallel operations using `onResult`, `onSuccess` - fire-and-forget style
- All promises use virtual threads for non-blocking execution

### Distributed Consensus (Rabia Algorithm)
- **Leaderless**: No single point of failure, leader elected deterministically only for cluster-wide operations
- **Batch-based**: Commands processed in batches with correlation IDs for tracking
- **Message Router**: Targeted delivery rather than broadcast, enables loose coupling
- **State Machine Interface**: Pluggable application logic, currently includes KV Store implementation

## Examples Implementation Patterns

### Endpoint Architecture
Each endpoint follows a consistent pattern:

1. **Interface Definition**
   - Single `perform()` method returning `Promise<ValidRequestType>`
   - Static `create()` factory method with local record implementation
   - Delegation to validation logic + `.async()` conversion

2. **Validation Architecture**
   - **Validation Logic**: Placed in `ValidRequestType.parse()` static method
   - **Error Hierarchy**: Sealed interfaces in separate `Errors.java` file
   - **Function Composition**: Using `Verify.ensureFn()` for reusable validations

3. **Naming Conventions**
   - **Validated Objects**: `Valid<RequestObjectName>` pattern
   - **Error Classes**: Nested sealed interfaces with factory methods
   - **Package Structure**: One package per endpoint

### Verification Patterns

#### Using ensureFn for Reusable Validations
```java
// Pattern: Create validation function and apply it
.flatMap(ensureFn(ErrorFactory::errorMethod, Verify.Is::predicate, params))
```

#### Error Factory Pattern
```java
// Consistent factory methods accepting context
static ErrorType errorMethod(String contextValue) {
    return STATIC_INSTANCE; // or new ErrorType(message + contextValue)
}
```

#### Result Composition Pattern
```java
// Combine multiple validations
Result.all(validation1(...), validation2(...), validation3(...))
      .map(ValidType::new)
```

### Error Hierarchy Design
- **Root Interface**: Extends `Cause`
- **Category Interfaces**: Sealed interfaces for error types
- **Concrete Records**: Final error implementations
- **Factory Methods**: Static methods for error creation
- **Context Preservation**: Factory methods accept context parameters

### Promise Integration
- **Primary Return Type**: `Promise<T>` for all endpoints
- **Conversion**: Use `Result.async()` to convert from validation
- **Extension Point**: TODO comments show where business logic goes

## Java 23 Features
- Uses `--enable-preview` for latest language features including JEP 467 Markdown Documentation Comments
- Virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`
- Pattern matching in switch expressions  
- Records for data classes (`Some`, `None`, `Success`, `Failure`)
- Sealed interfaces for exhaustive pattern matching
- Markdown Documentation Comments with `///` syntax (JEP 467)