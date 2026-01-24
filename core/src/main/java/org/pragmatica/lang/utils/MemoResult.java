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

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// A memoization utility for Result-returning computations.
///
/// <h2>Key Features</h2>
/// <ul>
///   <li><b>Success caching</b> - Only successful results are cached; failures are re-computed on each call</li>
///   <li><b>Thread-safe</b> - Safe for concurrent access from multiple threads</li>
///   <li><b>LRU eviction</b> - Optional size limit with least-recently-used eviction policy</li>
///   <li><b>Observability</b> - Hit/miss counters for monitoring cache effectiveness</li>
/// </ul>
///
/// <h2>Usage Example</h2>
/// <pre>{@code
/// // Unbounded cache
/// var cache = MemoResult.memoResult(key -> fetchFromDatabase(key));
/// cache.get("user-123");  // Computes and caches on success
/// cache.get("user-123");  // Returns cached value
///
/// // Bounded cache with LRU eviction
/// var boundedCache = MemoResult.memoResult(key -> expensiveComputation(key), 100);
/// }</pre>
///
/// <h2>Failure Semantics</h2>
/// Failures are NOT cached. Each call with a key that previously failed will re-invoke
/// the computation, allowing for transient failures to be retried automatically.
///
/// @param <K> the type of keys
/// @param <V> the type of cached values
public interface MemoResult<K, V> {
    /// Retrieves the cached value for the key, or computes it if not present.
    ///
    /// <p>If the computation fails, the failure is returned but NOT cached,
    /// allowing subsequent calls to retry the computation.
    ///
    /// @param key the key to look up or compute
    /// @return the cached or computed result
    Result<V> get(K key);

    /// Removes the cached value for the specified key.
    ///
    /// @param key the key to invalidate
    void invalidate(K key);

    /// Removes all cached values.
    void invalidateAll();

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
    /// <p>The cache grows without limit. Use {@link #memoResult(Fn1, int)} for bounded caches.
    ///
    /// @param computation the function to compute values for missing keys
    /// @param <K> the key type
    /// @param <V> the value type
    /// @return a new unbounded MemoResult instance
    static <K, V> MemoResult<K, V> memoResult(Fn1<Result<V>, K> computation) {
        return new UnboundedMemoResult<>(computation);
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
    /// @return a new bounded MemoResult instance
    /// @throws IllegalArgumentException if maxSize is not positive
    static <K, V> MemoResult<K, V> memoResult(Fn1<Result<V>, K> computation, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive, got: " + maxSize);
        }
        return new BoundedMemoResult<>(computation, maxSize);
    }
}

final class UnboundedMemoResult<K, V> implements MemoResult<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final Fn1<Result<V>, K> computation;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    UnboundedMemoResult(Fn1<Result<V>, K> computation) {
        this.computation = computation;
    }

    @Override
    public Result<V> get(K key) {
        var cached = cache.get(key);
        if (cached != null) {
            hits.incrementAndGet();
            return Result.success(cached);
        }
        misses.incrementAndGet();
        return computation.apply(key)
                          .onSuccess(value -> cache.put(key, value));
    }

    @Override
    public void invalidate(K key) {
        cache.remove(key);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
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

final class BoundedMemoResult<K, V> implements MemoResult<K, V> {
    private final Map<K, V> cache;
    private final Fn1<Result<V>, K> computation;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    BoundedMemoResult(Fn1<Result<V>, K> computation, int maxSize) {
        this.computation = computation;
        // LinkedHashMap with access-order for LRU, wrapped for thread safety
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                                                     return size() > maxSize;
                                                 }
        });
    }

    @Override
    public Result<V> get(K key) {
        V cached;
        synchronized (cache) {
            cached = cache.get(key);
        }
        if (cached != null) {
            hits.incrementAndGet();
            return Result.success(cached);
        }
        misses.incrementAndGet();
        return computation.apply(key)
                          .onSuccess(value -> {
                              synchronized (cache) {
                                  cache.put(key, value);
                              }
                          });
    }

    @Override
    public void invalidate(K key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    @Override
    public void invalidateAll() {
        synchronized (cache) {
            cache.clear();
        }
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
