# The Sequencer Pattern in PFJ

## Core Concept

The Sequencer pattern leverages monads to create a sequential processing pipeline where:

1. Each step depends on the successful completion of the previous step
2. Error handling is built into the pipeline
3. The code reads linearly despite handling complex conditional logic

## Implementation in Pragmatica Lite Core

All three monads (`Option<T>`, `Result<T>`, and `Promise<T>`) implement this pattern through their transformation methods:

```java
// Using Result<T> as a sequencer
Result<FinalOutput> process(Input input) {
    return Result.success(input)
        .flatMap(this::validateInput)      // Step 1
        .flatMap(this::enrichWithExternalData)  // Step 2
        .flatMap(this::transform)         // Step 3
        .map(this::formatOutput);         // Step 4
}
```

The key characteristics here:

- **Short-circuiting**: If any step returns a failure, subsequent steps are skipped
- **Error propagation**: Errors flow through the chain without explicit handling at each step
- **Linear readability**: The sequence of operations is immediately clear

## Why This Pattern is Powerful

1. **Mental Model Simplification**: Transforms complex branching logic into a linear sequence

   ```java
   // Without sequencer - complex nesting
   if (validateInput(input) != null) {
       var enriched = enrichWithExternalData(input);
       if (enriched != null) {
           var transformed = transform(enriched);
           if (transformed != null) {
               return formatOutput(transformed);
           }
       }
   }
   return null;
   
   // With sequencer - linear flow
   return Option.option(input)
       .flatMap(this::validateInput)
       .flatMap(this::enrichWithExternalData)
       .flatMap(this::transform)
       .map(this::formatOutput);
   ```

2. **Separation of Concerns**: Each step in the sequence can focus on a single responsibility

3. **Composability**: Sequences can be composed of smaller sequences

   ```java
   // Breaking down a complex sequence into modular parts
   Result<EnrichedData> enrichData(RawData data) {
       return Result.success(data)
           .flatMap(this::fetchAdditionalInfo)
           .flatMap(this::mergeData);
   }
   
   Result<Output> processRequest(Request request) {
       return Result.success(request)
           .flatMap(this::validateRequest)
           .flatMap(this::extractRawData)
           .flatMap(this::enrichData)  // Reusing a smaller sequence
           .flatMap(this::finalizeOutput);
   }
   ```

4. **Testability**: Each step can be tested in isolation, and sequences can be tested as a whole

## Advanced Sequencer Patterns

### 1. Error Recovery Within Sequences

```java
Result<Data> fetchWithRetry(String id) {
    return primaryDataSource.fetch(id)
        .recover(error -> backupDataSource.fetch(id))
        .mapError(error -> Causes.wrap(error, "All data sources failed"));
}
```

### 2. Conditional Branching Within Sequences

```java
Result<Document> processDocument(Document doc) {
    return Result.success(doc)
        .flatMap(document -> document.needsPreprocessing() 
            ? preprocess(document) 
            : Result.success(document))
        .flatMap(this::analyze);
}
```

### 3. Collecting and Processing in Batch

```java
Result<List<ProcessedItem>> processItems(List<RawItem> items) {
    return items.stream()
        .map(this::processItem)  // Returns Result<ProcessedItem>
        .collect(ResultCollector.toList());
}
```

### 4. Asynchronous Sequencing with Promise

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.promise(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            // Fan out to multiple concurrent operations
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            
            // Join results when both complete
            return Promise.all(p1, p2)
                .flatMap(tuple -> combine(tuple._1(), tuple._2()));
        });
}
```

The Sequencer pattern is particularly valuable for implementing business processes, data transformation pipelines, and validation workflows where each step builds upon the previous one. It's one of the most concrete examples of how functional thinking can lead to cleaner, more maintainable code in everyday Java development.

User:
please, fix the example with Promise.all: tuple access is incorrect. Check type which is returned by Promise.all.

Assistant:
You're absolutely right, my example contained an error in how I accessed the tuple returned by `Promise.all`. Let me fix that:

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.promise(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            // Fan out to multiple concurrent operations
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            
            // Join results when both complete
            return Promise.all(p1, p2)
                .flatMap(tuple -> {
                    var dataA = tuple.first();
                    var dataB = tuple.second();
                    return combine(dataA, dataB);
                });
        });
}
```

The key correction here is that the `Promise.all` method for two promises returns a `Promise<Tuple2<T1, T2>>` where `Tuple2` has accessor methods like `first()` and `second()` to get the individual values (rather than the incorrect `_1()`, `_2()` notation I used previously).

This properly demonstrates how to fan out concurrent operations and then join their results in the Sequencer pattern with `Promise<T>`.

User:
Please, try again. Innermost lambda receives two parameters.

Assistant:
You're absolutely right, and I apologize for the confusion. Let me correct the example properly:

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.promise(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            // Fan out to multiple concurrent operations
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            
            // Join results when both complete
            return Promise.all(p1, p2)
                .flatMap((dataA, dataB) -> combine(dataA, dataB));
        });
}
```

The key correction is that when using `Promise.all(p1, p2)`, the `flatMap` method receives the resolved values directly as multiple parameters, not as a tuple. This is a convenient design in the Promise API that allows for cleaner code without needing to access tuple elements.

Thank you for the correction - this properly demonstrates the fan-out and join pattern using the `Promise.all` method in the Pragmatica Lite Core library.

User:
Great. Update example and use three data components instead of two.

Assistant:
Here's the updated example with three data components instead of two:

```java
Promise<FinalResult> processConcurrently(Input input) {
    return Promise.promise(input)
        .flatMap(this::validateAsync)
        .flatMap(validated -> {
            // Fan out to three concurrent operations
            var p1 = fetchDataA(validated);
            var p2 = fetchDataB(validated);
            var p3 = fetchDataC(validated);
            
            // Join results when all three complete
            return Promise.all(p1, p2, p3)
                .flatMap((dataA, dataB, dataC) -> 
                    combine(dataA, dataB, dataC));
        });
}
```

This example demonstrates how `Promise.all` scales elegantly to handle any number of promises. When used with three promises, the `flatMap` receives three parameters directly, making it very readable and eliminating the need for tuple destructuring.

This pattern is particularly useful for complex operations that require data from multiple sources but can be fetched concurrently, such as:
- Gathering user profile data from different microservices
- Performing multiple database queries that don't depend on each other
- Making parallel API calls to external services

The elegance of this approach is that it maintains the linear readability of the Sequencer pattern while efficiently handling concurrent operations.
