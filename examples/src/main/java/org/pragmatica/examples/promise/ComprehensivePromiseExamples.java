package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.io.CoreError;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive examples demonstrating Promise monad usage patterns.
 * 
 * This class provides real-world examples of how to use Promise effectively
 * for asynchronous programming in Java, covering basic operations, error handling,
 * composition patterns, and integration with other monads.
 */
public class ComprehensivePromiseExamples {

    // ========================================
    // Basic Promise Operations
    // ========================================
    
    /**
     * Example 1: Creating and resolving promises
     */
    public void basicPromiseOperations() {
        // Create resolved promise
        Promise<String> resolved = Promise.resolved(Result.success("Hello, World!"));
        
        // Create failed promise
        Promise<String> failed = Promise.failure(CoreError.exception(new RuntimeException("Error occurred")));
        
        // Create promise from computation
        Promise<Integer> computed = Promise.promise(() -> {
            try {
                // Simulate some work
                Thread.sleep(100);
                return Result.success(42);
            } catch (Exception e) {
                return Result.failure(CoreError.exception(e));
            }
        });
        
        // Transform promise value
        Promise<String> transformed = computed.map(value -> "Result: " + value);
        
        // Chain promises
        Promise<String> chained = transformed.flatMap(str -> 
            Promise.promise(() -> Result.success(str.toUpperCase()))
        );
    }
    
    /**
     * Example 2: Error handling patterns
     */
    public void errorHandlingPatterns() {
        Promise<String> riskyOperation = Promise.promise(() -> {
            if (Math.random() > 0.5) {
                return Result.failure(CoreError.exception(new RuntimeException("Random failure")));
            }
            return Result.success("Success!");
        });
        
        // Handle errors with recovery
        Promise<String> withRecovery = riskyOperation
            .recover(error -> "Recovered from: " + error.message());
            
        // Transform errors
        Promise<String> withErrorTransform = riskyOperation
            .mapError(error -> CoreError.exception(new IllegalStateException("Wrapped: " + error.message())));
            
        // Provide fallback
        Promise<String> withFallback = riskyOperation
            .orElse(() -> Promise.resolved(Result.success("Default value")));
    }
    
    // ========================================
    // Composition Patterns
    // ========================================
    
    /**
     * Example 3: Promise composition - all, race, sequence
     */
    public void compositionPatterns() {
        List<Promise<String>> promises = List.of(
            Promise.promise(() -> { try { Thread.sleep(100); return Result.success("First"); } catch (Exception e) { return Result.failure(CoreError.exception(e)); } }),
            Promise.promise(() -> { try { Thread.sleep(200); return Result.success("Second"); } catch (Exception e) { return Result.failure(CoreError.exception(e)); } }),
            Promise.promise(() -> { try { Thread.sleep(150); return Result.success("Third"); } catch (Exception e) { return Result.failure(CoreError.exception(e)); } })
        );
        
        // Wait for all to complete
        Promise<List<Result<String>>> allResults = Promise.allOf(promises);
        
        // Race - first to complete wins
        Promise<String> firstResult = Promise.any(promises);
        
        // Sequential execution
        Promise<String> sequential = Promise.resolved(Result.success("Start"))
            .flatMap(start -> Promise.promise(() -> Result.success(start + " -> Step 1")))
            .flatMap(step1 -> Promise.promise(() -> Result.success(step1 + " -> Step 2")))
            .flatMap(step2 -> Promise.promise(() -> Result.success(step2 + " -> Complete")));
    }
    
    /**
     * Example 4: Integration with Result and Option
     */
    public void monadIntegration() {
        // Promise<Result<T>> pattern for operations that can fail
        Promise<Result<String>> resultPromise = Promise.promise(() -> {
            try {
                // Some operation that might fail
                return Result.success(Result.success("Operation completed"));
            } catch (Exception e) {
                return Result.success(Result.failure(CoreError.exception(e)));
            }
        });
        
        // Promise<Option<T>> pattern for optional values
        Promise<Option<String>> optionPromise = Promise.promise(() -> {
            String value = findValueSomewhere();
            return Result.success(Option.option(value));
        });
        
        // Flatten nested structures
        Promise<String> flattened = resultPromise
            .flatMap(result -> result.fold(
                error -> Promise.failure(error),
                success -> Promise.resolved(Result.success(success))
            ));
    }
    
    // ========================================
    // Real-World Scenarios
    // ========================================
    
    /**
     * Example 5: Async I/O operations
     */
    public void asyncIOOperations() {
        // Simulate async file reading
        Promise<String> fileContent = Promise.promise(() -> {
            try {
                // In real code, this would be actual async I/O
                Thread.sleep(500); // Simulate I/O delay
                return Result.success("File content...");
            } catch (Exception e) {
                return Result.failure(CoreError.exception(e));
            }
        });
        
        // Process file content
        Promise<List<String>> processedLines = fileContent
            .map(content -> List.of(content.split("\n")))
            .map(lines -> lines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList());
    }
    
    /**
     * Example 6: Parallel processing
     */
    public void parallelProcessing() {
        List<Integer> data = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Process items in parallel
        List<Promise<Integer>> parallelWork = data.stream()
            .map(item -> Promise.promise(() -> {
                try {
                    // Simulate expensive computation
                    Thread.sleep(100);
                    return Result.success(item * item);
                } catch (Exception e) {
                    return Result.failure(CoreError.exception(e));
                }
            }))
            .toList();
            
        // Collect results
        Promise<List<Result<Integer>>> allResults = Promise.allOf(parallelWork);
        
        // Process results
        Promise<Integer> sum = allResults.map(results -> 
            results.stream()
                .mapToInt(result -> result.or(0))
                .sum());
    }
    
    /**
     * Example 7: Timeout and cancellation
     */
    public void timeoutAndCancellation() {
        Promise<String> longRunningTask = Promise.promise(() -> {
            try {
                Thread.sleep(5000); // 5 second task
                return Result.success("Task completed");
            } catch (Exception e) {
                return Result.failure(CoreError.exception(e));
            }
        });
        
        // Add timeout (Note: timeout() method not available in current API)
        Promise<String> withTimeout = longRunningTask
            .recover(error -> "Task timed out");
    }
    
    /**
     * Example 8: Migration from CompletableFuture
     */
    public void migrationFromCompletableFuture() {
        // Old CompletableFuture approach
        CompletableFuture<String> oldWay = CompletableFuture
            .supplyAsync(() -> "Hello")
            .thenApply(s -> s + " World")
            .exceptionally(throwable -> "Error: " + throwable.getMessage());
            
        // New Promise approach
        Promise<String> newWay = Promise.promise(() -> Result.success("Hello"))
            .map(s -> s + " World")
            .recover(error -> "Error: " + error.message());
            
        // Note: Promise.from() is not available in current API
        // CompletableFuture integration would need custom implementation
    }
    
    // Helper method for examples
    private String findValueSomewhere() {
        return Math.random() > 0.5 ? "Found value" : null;
    }
}