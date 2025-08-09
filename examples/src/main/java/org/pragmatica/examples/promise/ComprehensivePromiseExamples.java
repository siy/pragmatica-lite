package org.pragmatica.examples.promise;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Option;

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
        Promise<String> resolved = Promise.resolved("Hello, World!");
        
        // Create failed promise
        Promise<String> failed = Promise.failed(new RuntimeException("Error occurred"));
        
        // Create promise from computation
        Promise<Integer> computed = Promise.async(() -> {
            // Simulate some work
            Thread.sleep(100);
            return 42;
        });
        
        // Transform promise value
        Promise<String> transformed = computed.map(value -> "Result: " + value);
        
        // Chain promises
        Promise<String> chained = transformed.flatMap(str -> 
            Promise.async(() -> str.toUpperCase())
        );
    }
    
    /**
     * Example 2: Error handling patterns
     */
    public void errorHandlingPatterns() {
        Promise<String> riskyOperation = Promise.async(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("Random failure");
            }
            return "Success!";
        });
        
        // Handle errors with recovery
        Promise<String> withRecovery = riskyOperation
            .recover(error -> "Recovered from: " + error.getMessage());
            
        // Transform errors
        Promise<String> withErrorTransform = riskyOperation
            .mapError(error -> new IllegalStateException("Wrapped: " + error.getMessage()));
            
        // Provide fallback
        Promise<String> withFallback = riskyOperation
            .orElse(() -> Promise.resolved("Default value"));
    }
    
    // ========================================
    // Composition Patterns
    // ========================================
    
    /**
     * Example 3: Promise composition - all, race, sequence
     */
    public void compositionPatterns() {
        List<Promise<String>> promises = List.of(
            Promise.async(() -> { Thread.sleep(100); return "First"; }),
            Promise.async(() -> { Thread.sleep(200); return "Second"; }),
            Promise.async(() -> { Thread.sleep(150); return "Third"; })
        );
        
        // Wait for all to complete
        Promise<List<String>> allResults = Promise.all(promises);
        
        // Race - first to complete wins
        Promise<String> firstResult = Promise.race(promises);
        
        // Sequential execution
        Promise<String> sequential = Promise.resolved("Start")
            .flatMap(start -> Promise.async(() -> start + " -> Step 1"))
            .flatMap(step1 -> Promise.async(() -> step1 + " -> Step 2"))
            .flatMap(step2 -> Promise.async(() -> step2 + " -> Complete"));
    }
    
    /**
     * Example 4: Integration with Result and Option
     */
    public void monadIntegration() {
        // Promise<Result<T>> pattern for operations that can fail
        Promise<Result<String>> resultPromise = Promise.async(() -> {
            try {
                // Some operation that might fail
                return Result.success("Operation completed");
            } catch (Exception e) {
                return Result.failure(e.getMessage());
            }
        });
        
        // Promise<Option<T>> pattern for optional values
        Promise<Option<String>> optionPromise = Promise.async(() -> {
            String value = findValueSomewhere();
            return Option.option(value);
        });
        
        // Flatten nested structures
        Promise<String> flattened = resultPromise
            .flatMap(result -> result.fold(
                error -> Promise.failed(new RuntimeException(error)),
                success -> Promise.resolved(success)
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
        Promise<String> fileContent = Promise.async(() -> {
            // In real code, this would be actual async I/O
            Thread.sleep(500); // Simulate I/O delay
            return "File content...";
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
            .map(item -> Promise.async(() -> {
                // Simulate expensive computation
                Thread.sleep(100);
                return item * item;
            }))
            .toList();
            
        // Collect results
        Promise<List<Integer>> allResults = Promise.all(parallelWork);
        
        // Process results
        Promise<Integer> sum = allResults.map(results -> 
            results.stream().mapToInt(Integer::intValue).sum());
    }
    
    /**
     * Example 7: Timeout and cancellation
     */
    public void timeoutAndCancellation() {
        Promise<String> longRunningTask = Promise.async(() -> {
            Thread.sleep(5000); // 5 second task
            return "Task completed";
        });
        
        // Add timeout
        Promise<String> withTimeout = longRunningTask
            .timeout(Duration.ofSeconds(2))
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
        Promise<String> newWay = Promise.async(() -> "Hello")
            .map(s -> s + " World")
            .recover(error -> "Error: " + error.getMessage());
            
        // Convert CompletableFuture to Promise
        Promise<String> converted = Promise.from(oldWay);
    }
    
    // Helper method for examples
    private String findValueSomewhere() {
        return Math.random() > 0.5 ? "Found value" : null;
    }
}