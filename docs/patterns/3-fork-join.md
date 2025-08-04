# Fork-Join Pattern

## Core Concept

The Fork-Join pattern is a way to encode independence of the operations. This in turn makes
possible execution of operations concurrently, if necessary. Even if concurrent execution is not used, preserving information about 
the lack of the data dependencies reduces mental overhead and simplifies reasoning about the code.

## Implementation in Pragmatica Lite Core

This pattern is elegantly implemented using `Promise.all()` or `Result.all()` which take multiple promises and joins their results:

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.promise(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            // Fan-Out: start three concurrent operations
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            var p3 = fetchDataC(validated);
            
            // Fan-In: join results when all three complete
            return Promise.all(p1, p2, p3)
                .flatMap((dataA, dataB, dataC) -> 
                    combine(dataA, dataB, dataC));
        });
}
```

## Key Characteristics

1. **Concurrent Execution**: Multiple independent operations execute in parallel

2. **Resource Efficiency**: Makes optimal use of available compute resources

3. **Clean Syntax**: The code remains clean and declarative despite the concurrency

4. **Automatic Synchronization**: No need for explicit synchronization mechanisms

## Advanced Fan-Out-Fan-In Patterns

### 1. Dynamic Fan-Out

```java
Promise<List<Result>> processAllConcurrently(List<Input> inputs) {
    // Dynamically create a list of promises based on inputs
    List<Promise<Result>> promises = inputs.stream()
        .map(this::processAsync)
        .collect(Collectors.toList());
    
    // Join all results when complete
    return Promise.all(promises);
}
```

### 2. Fan-Out with Throttling

```java
Promise<List<Result>> processWithThrottling(List<Input> inputs) {
    // Process with bounded concurrency (e.g., 5 at a time)
    return inputs.stream()
        .collect(batched(5))
        .map(batch -> {
            List<Promise<Result>> batchPromises = batch.stream()
                .map(this::processAsync)
                .collect(Collectors.toList());
            return Promise.all(batchPromises);
        })
        .reduce(Promise.success(new ArrayList<>()), 
            (accPromise, batchPromise) -> 
                accPromise.flatMap(acc -> 
                    batchPromise.map(results -> {
                        acc.addAll(results);
                        return acc;
                    })));
}
```

### 3. Conditional Fan-In

```java
Promise<OptimizedResult> getOptimizedResult(Input input) {
    // Fan-out to multiple services
    var p1 = service1.process(input);
    var p2 = service2.process(input);
    var p3 = service3.process(input);
    
    // Fan-in with racing - take the first successful result
    return Promise.any(p1, p2, p3)
        .recover(error -> {
            // Fallback if all fail
            return Promise.success(OptimizedResult.getDefault());
        });
}
```

### 4. Heterogeneous Fan-Out

```java
Promise<Dashboard> buildDashboard(String userId) {
    // Fan-out to different types of data
    Promise<UserProfile> profilePromise = userService.getProfile(userId);
    Promise<List<Transaction>> txPromise = transactionService.getRecent(userId);
    Promise<Recommendations> recPromise = recommendationEngine.getFor(userId);
    
    // Fan-in to combine different types into a cohesive result
    return Promise.all(profilePromise, txPromise, recPromise)
        .flatMap((profile, transactions, recommendations) -> 
            dashboardRenderer.create(profile, transactions, recommendations));
}
```

## Why This Pattern is Powerful

1. **Performance Optimization**: Dramatically improves response times for independent operations

2. **Resource Utilization**: Makes better use of multi-core processors and I/O parallelism

3. **Clean Expression**: Expresses concurrent operations without complex synchronization code

4. **Error Propagation**: Errors from any parallel operation are properly propagated

The Fan-Out-Fan-In pattern is one of the most impactful patterns in PFJ, turning what would be complex concurrent code in traditional imperative programming into clean, declarative pipelines. It's especially valuable in modern systems that interact with multiple external services or APIs.

