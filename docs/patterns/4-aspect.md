# Aspect Pattern in PFJ

## Core Concept

The Aspect pattern in Pragmatic Functional Java provides a clean, functional approach to cross-cutting concerns similar to Aspect-Oriented Programming. It allows wrapping operations with pre-processing and post-processing behavior without cluttering the main business logic.

## Implementation in Pragmatica Lite Core

This pattern is implemented using higher-order functions that transform operations by adding behavior before and/or after their execution:

```java
// Define an aspect that adds logging to any operation
Function<Promise<T>, Promise<T>> loggingAspect(String operationName) {
    return operation -> Promise.promise()
        .peek(__ -> log.info("Starting: {}", operationName))
        .flatMap(__ -> operation)
        .peek(result -> log.info("Completed: {} with result: {}", operationName, result))
        .peekError(error -> log.error("Failed: {} with error: {}", operationName, error));
}

// Apply the aspect to an operation
Promise<UserProfile> getUserProfile(String userId) {
    Promise<UserProfile> operation = userService.fetchProfile(userId);
    return loggingAspect("FetchUserProfile").apply(operation);
}
```

## Key Characteristics

1. **Separation of Concerns**: Keeps cross-cutting concerns separate from business logic
2. **Composable**: Aspects can be stacked and combined
3. **Non-invasive**: Business logic remains clean and focused
4. **Reusable**: Aspects can be applied to different operations

## Common Use Cases

### 1. Logging and Metrics

```java
Function<Promise<T>, Promise<T>> metricsAspect(String operationName) {
    return operation -> {
        long startTime = System.currentTimeMillis();
        return operation
            .peek(result -> {
                long duration = System.currentTimeMillis() - startTime;
                metrics.record(operationName, duration);
            });
    };
}
```

### 2. Security and Authorization

```java
Function<Promise<Document>, Promise<Document>> securityAspect(User user) {
    return operation -> {
        if (!authService.canAccess(user, "documents")) {
            return Promise.failure(AccessDeniedException.of("User not authorized"));
        }
        return operation
            .flatMap(document -> {
                if (!document.isVisibleTo(user)) {
                    return Promise.failure(AccessDeniedException.of("Document not accessible"));
                }
                return Promise.success(document);
            });
    };
}
```

### 3. Caching

```java
Function<Promise<Data>, Promise<Data>> cachingAspect(String cacheKey, Duration ttl) {
    return operation -> {
        return Promise.promise()
            .flatMap(__ -> {
                // Check cache first
                return cacheService.get(cacheKey)
                    .recover(__ -> {
                        // Cache miss - execute operation and cache result
                        return operation
                            .peek(result -> cacheService.put(cacheKey, result, ttl));
                    });
            });
    };
}
```

### 4. Retries

```java
Function<Promise<T>, Promise<T>> retryAspect(int maxRetries, Duration delay) {
    return operation -> {
        return retry(operation, 0, maxRetries, delay);
    };
}

private Promise<T> retry(Promise<T> operation, int attempt, int maxRetries, Duration delay) {
    return operation.recover(error -> {
        if (error instanceof RetryableException && attempt < maxRetries) {
            return Promise.delay(delay)
                .flatMap(__ -> retry(operation, attempt + 1, maxRetries, delay));
        }
        return Promise.failure(error);
    });
}
```

### 5. Transaction Management (Simplified Example)

```java
Function<Promise<T>, Promise<T>> transactionalAspect() {
    return operation -> {
        return Promise.promise()
            .flatMap(__ -> {
                Transaction tx = db.beginTransaction();
                return operation
                    .peek(result -> tx.commit())
                    .recover(error -> {
                        tx.rollback();
                        return Promise.failure(error);
                    });
            });
    };
}
```

## Composing Aspects

One of the powerful features of the Aspect pattern is the ability to compose multiple aspects:

```java
// Create composite aspect
Function<Promise<T>, Promise<T>> compositeAspect = 
    loggingAspect("UserProfileFetch")
        .andThen(metricsAspect("user.profile.fetch"))
        .andThen(cachingAspect("user:" + userId, Duration.ofMinutes(5)))
        .andThen(retryAspect(3, Duration.ofMillis(100)));

// Apply composite aspect to operation
Promise<UserProfile> getUserProfile(String userId) {
    Promise<UserProfile> operation = userService.fetchProfile(userId);
    return compositeAspect.apply(operation);
}
```

## Why This Pattern is Powerful

1. **Functional Approach to AOP**: Achieves AOP-like benefits within a functional paradigm
2. **Explicit Application**: Makes aspect application visible in the code
3. **Testing**: Aspects can be easily separated for testing
4. **Flexibility**: Works with both synchronous and asynchronous operations

The Aspect pattern complements the other patterns we've discussed by providing a clean way to handle cross-cutting concerns that would otherwise pollute the main logic expressed through the Sequencer and Fan-Out-Fan-In patterns.
