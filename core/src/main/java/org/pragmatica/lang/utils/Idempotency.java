/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.lang.utils;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/// A utility for ensuring operations are executed at most once per key within a configurable TTL window.
/// This implementation provides idempotency guarantees for asynchronous operations, preventing duplicate
/// executions and enabling safe retries from clients.
///
/// <h2>Key Features</h2>
/// <ul>
///   <li><b>At-most-once execution</b> - Operations with the same key execute only once within the TTL window</li>
///   <li><b>In-flight deduplication</b> - Concurrent calls with the same key share a single Promise (coalesce)</li>
///   <li><b>Result caching</b> - Successful results are cached and returned immediately for subsequent calls</li>
///   <li><b>Failure transparency</b> - Failed operations are NOT cached, allowing immediate retry</li>
///   <li><b>Automatic cleanup</b> - Expired entries are periodically removed to prevent memory leaks</li>
/// </ul>
///
/// <h2>Thread Safety</h2>
/// The implementation is fully thread-safe. Multiple threads can simultaneously call {@code execute()}
/// with different or identical keys. Each protected operation scope should have its own Idempotency instance.
///
/// <h2>Usage Example</h2>
/// <pre>{@code
/// // Create an idempotency guard with 5-minute TTL
/// Idempotency.create(timeSpan(5).minutes())
///     .onFailure(cause -> log.error("Failed to create: {}", cause.message()))
///     .onSuccess(idempotency -> {
///         // Basic usage - operation executes at most once per requestId
///         idempotency.execute(requestId, () -> processPayment(order))
///             .onSuccess(receipt -> log.info("Payment processed: {}", receipt))
///             .onFailure(cause -> log.error("Payment failed: {}", cause.message()));
///     });
/// }</pre>
///
/// <h2>Behavior Matrix</h2>
/// <table border="1">
///   <tr><th>Scenario</th><th>Behavior</th></tr>
///   <tr><td>First call with key K</td><td>Execute operation, cache result on success</td></tr>
///   <tr><td>Concurrent call with key K (in-flight)</td><td>Return same Promise (coalesce)</td></tr>
///   <tr><td>Subsequent call with key K (cached)</td><td>Return cached result immediately</td></tr>
///   <tr><td>Call after TTL expires</td><td>Execute operation again (fresh start)</td></tr>
///   <tr><td>Call after failure</td><td>Execute operation again (failures not cached)</td></tr>
/// </table>
///
/// <h2>Factory Methods</h2>
/// <pre>{@code
/// // Basic creation with TTL
/// Idempotency.create(timeSpan(10).minutes());
///
/// // With custom time source (for testing)
/// Idempotency.create(timeSpan(10).minutes(), testTimeSource);
/// }</pre>
///
/// <h2>TTL Semantics</h2>
/// TTL is measured from request time, not operation completion time. In-flight operations
/// (unresolved Promises) are never expired - TTL only applies after the operation completes.
/// This ensures concurrent requests share the same Promise without risk of mid-flight expiration.
///
/// <h2>Memory Considerations</h2>
/// The cache is unbounded - entries are only removed on TTL expiration or failure. Callers must
/// ensure key cardinality is bounded (e.g., by using user IDs, session IDs, or rate-limited request IDs).
/// For high-cardinality scenarios, consider adding an upstream rate limiter or using bounded keys.
///
/// <h2>Integration with Retry</h2>
/// Idempotency works well with {@link Retry} for client-side retry logic:
/// <pre>{@code
/// // Server side: idempotent endpoint
/// Idempotency.create(timeSpan(5).minutes())
///     .onSuccess(idempotency -> {
///         public Promise<Receipt> processPayment(String requestId, PaymentRequest request) {
///             return idempotency.execute(requestId, () -> doProcessPayment(request));
///         }
///     });
///
/// // Client side: safe to retry
/// var retry = Retry.create().attempts(3).strategy(BackoffStrategy.exponential()...);
///
/// retry.execute(() -> paymentService.processPayment(requestId, request));
/// }</pre>
///
/// @see Retry
/// @see CircuitBreaker
public interface Idempotency {
    /// Executes an operation with idempotency guarantee based on the provided key.
    ///
    /// <p>This method ensures at-most-once execution semantics within the TTL window:
    /// <ul>
    ///   <li>If no cached result exists and no operation is in-flight, executes the operation</li>
    ///   <li>If an operation with the same key is in-flight, returns the same Promise (coalesces)</li>
    ///   <li>If a cached result exists and is not expired, returns it immediately</li>
    ///   <li>Failed operations are NOT cached, allowing immediate retry</li>
    /// </ul>
    ///
    /// <p><b>Example:</b>
    /// <pre>{@code
    /// // The payment will execute at most once per requestId within TTL
    /// idempotency.execute(requestId, () -> processPayment(order))
    ///     .onSuccess(receipt -> log.info("Processed: {}", receipt.id()));
    ///
    /// // Concurrent or subsequent calls with same requestId get the same result
    /// idempotency.execute(requestId, () -> processPayment(order))
    ///     .onSuccess(receipt -> log.info("Same receipt: {}", receipt.id()));
    /// }</pre>
    ///
    /// @param key       unique identifier for this operation invocation (e.g., request ID, transaction ID)
    /// @param operation the promise-returning operation to execute; will only be invoked once per key within TTL
    /// @param <T>       the return type of the operation
    ///
    /// @return a promise containing the result - either freshly computed, from cache, or shared with in-flight request
    <T> Promise<T> execute(String key, Supplier<Promise<T>> operation);

    /// Creates an Idempotency instance with the specified TTL using system time.
    ///
    /// <p>The cleanup interval is automatically derived from TTL: min(TTL/5, 1 minute)
    ///
    /// @param ttl the time-to-live for cached results; must be positive
    ///
    /// @return Result containing the Idempotency instance, or failure if TTL is invalid
    static Result<Idempotency> create(org.pragmatica.lang.io.TimeSpan ttl) {
        return create(ttl, TimeSource.system());
    }

    /// Creates an Idempotency instance with the specified TTL and time source.
    ///
    /// <p>The cleanup interval is automatically derived from TTL: min(TTL/5, 1 minute)
    ///
    /// <p>This factory is primarily useful for testing, allowing you to control time progression
    /// and verify TTL behavior without waiting for real time to pass.
    ///
    /// <p><b>Example (testing):</b>
    /// <pre>{@code
    /// var testTime = TimeSource.controllable();
    /// Idempotency.create(timeSpan(5).minutes(), testTime)
    ///     .onSuccess(idempotency -> {
    ///         idempotency.execute("key", operation);  // Executes
    ///         testTime.advance(timeSpan(6).minutes());
    ///         idempotency.execute("key", operation);  // Executes again (TTL expired)
    ///     });
    /// }</pre>
    ///
    /// @param ttl        the time-to-live for cached results; must be positive
    /// @param timeSource custom time source for TTL calculations
    ///
    /// @return Result containing the Idempotency instance, or failure if TTL is invalid
    static Result<Idempotency> create(org.pragmatica.lang.io.TimeSpan ttl, TimeSource timeSource) {
        if (ttl.nanos() <= 0) {
            return new IdempotencyError.InvalidTtl(ttl).result();
        }
        return Result.success(createIdempotency(ttl, timeSource));
    }

    /// Error types that can occur during idempotency creation.
    sealed interface IdempotencyError extends Cause {
        /// Indicates the TTL provided was not positive.
        record InvalidTtl(org.pragmatica.lang.io.TimeSpan ttl) implements IdempotencyError {
            @Override
            public String message() {
                return "TTL must be positive, got: " + ttl;
            }
        }
    }

    private static Idempotency createIdempotency(org.pragmatica.lang.io.TimeSpan ttl, TimeSource timeSource) {
        record CachedEntry<T>(Promise<T> promise, long expiresAtNanos) {
            boolean shouldReplace(long now) {
                return promise.isResolved() && now >= expiresAtNanos;
            }
        }
        record idempotency(long ttlNanos,
                           TimeSource timeSource,
                           ConcurrentHashMap<String, CachedEntry<?>> entries) implements Idempotency {
            private static final Logger log = LoggerFactory.getLogger(Idempotency.class);

            @Override
            @SuppressWarnings("unchecked")
            public <T> Promise<T> execute(String key, Supplier<Promise<T>> operation) {
                var now = timeSource.nanoTime();
                var created = new AtomicBoolean(false);
                var entry = (CachedEntry<T>) entries.compute(key,
                                                             (k, existing) -> {
                                                                 if (existing != null && !existing.shouldReplace(now)) {
                                                                     return existing;
                                                                 }
                                                                 created.set(true);
                                                                 return new CachedEntry<>(Promise.promise(),
                                                                                          now + ttlNanos);
                                                             });
                if (!created.get()) {
                    log.trace("Returning existing entry for key: {}", key);
                    return entry.promise();
                }
                log.trace("Executing operation for key: {}", key);
                executeOperation(key, operation, entry);
                return entry.promise();
            }

            private <T> void executeOperation(String key,
                                              Supplier<Promise<T>> operation,
                                              CachedEntry<T> entry) {
                operation.get()
                         .onResult(result -> result.onSuccess(value -> handleSuccess(key, value, entry))
                                                   .onFailure(cause -> handleFailure(key, cause, entry)));
            }

            private <T> void handleSuccess(String key, T value, CachedEntry<T> entry) {
                log.trace("Operation succeeded for key: {}", key);
                entry.promise()
                     .succeed(value);
            }

            private <T> void handleFailure(String key, Cause cause, CachedEntry<T> entry) {
                log.trace("Operation failed for key: {}, cause: {}", key, cause.message());
                entry.promise()
                     .fail(cause);
                // Safe removal - only remove if still our entry
                entries.remove(key, entry);
            }
        }
        var entries = new ConcurrentHashMap<String, CachedEntry<?>>();
        // Cleanup interval: min(TTL/5, 1 minute)
        var oneMinuteNanos = timeSpan(1).minutes()
                                     .nanos();
        var cleanupIntervalNanos = Math.min(ttl.nanos() / 5, oneMinuteNanos);
        var cleanupInterval = timeSpan(cleanupIntervalNanos).nanos();
        // Schedule periodic cleanup using WeakReference to allow GC of the Idempotency instance.
        // When the entries map is GC'd, the cleanup task becomes a no-op.
        var log = LoggerFactory.getLogger(Idempotency.class);
        var entriesRef = new WeakReference<>(entries);
        SharedScheduler.scheduleAtFixedRate(() -> {
                                                try{
                                                    var map = entriesRef.get();
                                                    if (map == null) {
                                                        return;
                                                    }
                                                    var sizeBefore = map.size();
                                                    long now = timeSource.nanoTime();
                                                    map.entrySet()
                                                       .removeIf(e -> e.getValue()
                                                                       .shouldReplace(now));
                                                    var removed = sizeBefore - map.size();
                                                    if (removed > 0) {
                                                        log.trace("Cleanup removed {} expired entries", removed);
                                                    }
                                                } catch (Exception e) {
                                                    log.warn("Cleanup task failed", e);
                                                }
                                            },
                                            cleanupInterval);
        return new idempotency(ttl.nanos(), timeSource, entries);
    }
}
