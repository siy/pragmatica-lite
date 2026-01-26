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
import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// A memoization utility for Promise-returning computations.
///
/// <h2>Key Features</h2>
/// <ul>
///   <li><b>Success caching</b> - Only successful results are cached; failures are re-computed on each call</li>
///   <li><b>Thread-safe</b> - Safe for concurrent access from multiple threads</li>
///   <li><b>LRU eviction</b> - Optional size limit with least-recently-used eviction policy</li>
///   <li><b>Promise deduplication</b> - Concurrent requests for the same key get the same Promise instance</li>
///   <li><b>Observability</b> - Hit/miss counters for monitoring cache effectiveness</li>
/// </ul>
///
/// <h2>Usage Example</h2>
/// <pre>{@code
/// // Unbounded cache
/// var cache = MemoPromise.memoPromise(key -> fetchFromRemoteService(key));
/// cache.get("user-123");  // Computes and caches on success
/// cache.get("user-123");  // Returns cached Promise
///
/// // Bounded cache with LRU eviction
/// MemoPromise.memoPromise(key -> expensiveAsyncComputation(key), 100)
///            .onSuccess(cache -> cache.get("key"));
/// }</pre>
///
/// <h2>Failure Semantics</h2>
/// Failures are NOT cached. When a Promise completes with failure, the entry is removed
/// from the cache, allowing subsequent calls to retry the computation.
///
/// <h2>Promise Deduplication</h2>
/// Concurrent requests for the same key receive the same Promise instance, avoiding
/// duplicate computations. If the Promise fails, the entry is removed so subsequent
/// requests can retry.
///
/// @param <K> the type of keys
/// @param <V> the type of cached values
public interface MemoPromise<K, V> {
    /// Error cause for invalid maxSize parameter.
    Cause INVALID_MAX_SIZE = () -> "maxSize must be positive";

    /// Retrieves the cached Promise for the key, or computes it if not present.
    ///
    /// <p>Concurrent requests for the same key receive the same Promise instance.
    /// If the computation fails, the entry is removed from the cache,
    /// allowing subsequent calls to retry the computation.
    ///
    /// @param key the key to look up or compute
    /// @return the cached or computed Promise
    Promise<V> get(K key);

    /// Removes the cached entry for the specified key.
    ///
    /// @param key the key to invalidate
    /// @return Unit for composition
    Unit invalidate(K key);

    /// Removes all cached entries.
    ///
    /// @return Unit for composition
    Unit invalidateAll();

    /// Returns the number of cache hits since creation.
    ///
    /// @return the hit count
    long hitCount();

    /// Returns the number of cache misses since creation.
    ///
    /// @return the miss count
    long missCount();

    /// Returns the current number of cached entries.
    ///
    /// @return the cache size
    int size();

    /// Creates an unbounded memoization cache.
    ///
    /// <p>The cache grows without limit. Use {@link #memoPromise(Fn1, int)} for bounded caches.
    ///
    /// @param computation the function to compute values for missing keys
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return a new unbounded MemoPromise instance
    static <K, V> MemoPromise<K, V> memoPromise(Fn1<Promise<V>, K> computation) {
        Objects.requireNonNull(computation, "computation must not be null");
        return new UnboundedMemoPromise<>(computation);
    }

    /// Creates a bounded memoization cache with LRU eviction.
    ///
    /// <p>When the cache exceeds the specified size, the least recently accessed
    /// entry is evicted to make room for new entries.
    ///
    /// @param computation the function to compute values for missing keys
    /// @param maxSize the maximum number of entries to cache (must be positive)
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return Result containing the MemoPromise instance or failure if maxSize is invalid
    static <K, V> Result<MemoPromise<K, V>> memoPromise(Fn1<Promise<V>, K> computation, int maxSize) {
        Objects.requireNonNull(computation, "computation must not be null");
        if (maxSize <= 0) {
            return INVALID_MAX_SIZE.result();
        }
        return Result.success(new BoundedMemoPromise<>(computation, maxSize));
    }
}

final class UnboundedMemoPromise<K, V> implements MemoPromise<K, V> {
    private final ConcurrentHashMap<K, Promise<V>> cache = new ConcurrentHashMap<>();
    private final Fn1<Promise<V>, K> computation;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    UnboundedMemoPromise(Fn1<Promise<V>, K> computation) {
        this.computation = computation;
    }

    @Override
    public Promise<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");
        var cached = cache.get(key);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        // Create promise first, then put it into the cache
        // This avoids recursive update issues with ConcurrentHashMap
        misses.incrementAndGet();
        var promise = computation.apply(key);
        var existing = cache.putIfAbsent(key, promise);
        if (existing != null) {
            // Another thread inserted first, use their promise
            hits.incrementAndGet();
            misses.decrementAndGet();
            return existing;
        }
        // Attach failure handler after insertion to avoid recursive updates
        promise.onResult(result -> result.onFailure(_ -> cache.remove(key, promise)));
        return promise;
    }

    @Override
    public Unit invalidate(K key) {
        cache.remove(key);
        return Unit.unit();
    }

    @Override
    public Unit invalidateAll() {
        cache.clear();
        return Unit.unit();
    }

    @Override
    public long hitCount() {
        return hits.get();
    }

    @Override
    public long missCount() {
        return misses.get();
    }

    @Override
    public int size() {
        return cache.size();
    }
}

final class BoundedMemoPromise<K, V> implements MemoPromise<K, V> {
    private final Map<K, Promise<V>> cache;
    private final Fn1<Promise<V>, K> computation;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    BoundedMemoPromise(Fn1<Promise<V>, K> computation, int maxSize) {
        this.computation = computation;
        // LinkedHashMap with access-order for LRU, wrapped for thread safety
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Promise<V>> eldest) {
                                                     return size() > maxSize;
                                                 }
        });
    }

    @Override
    public Promise<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");
        Promise<V> cached;
        synchronized (cache) {
            cached = cache.get(key);
        }
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        // Create promise outside synchronized block to avoid holding lock during computation
        misses.incrementAndGet();
        var promise = computation.apply(key);
        Promise<V> existing;
        synchronized (cache) {
            existing = cache.putIfAbsent(key, promise);
        }
        if (existing != null) {
            // Another thread inserted first, use their promise
            hits.incrementAndGet();
            misses.decrementAndGet();
            return existing;
        }
        // Attach failure handler after insertion
        promise.onResult(result -> result.onFailure(_ -> {
            synchronized (cache) {
                cache.remove(key, promise);
            }
        }));
        return promise;
    }

    @Override
    public Unit invalidate(K key) {
        synchronized (cache) {
            cache.remove(key);
        }
        return Unit.unit();
    }

    @Override
    public Unit invalidateAll() {
        synchronized (cache) {
            cache.clear();
        }
        return Unit.unit();
    }

    @Override
    public long hitCount() {
        return hits.get();
    }

    @Override
    public long missCount() {
        return misses.get();
    }

    @Override
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
