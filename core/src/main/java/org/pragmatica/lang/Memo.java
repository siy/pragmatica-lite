/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
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
 */

package org.pragmatica.lang;

import org.pragmatica.lang.Functions.Fn1;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/// Pure synchronous memoization cache.
///
/// Caches the results of a computation function. Thread-safe for concurrent access.
/// Optionally supports LRU eviction when a maximum size is specified.
///
/// @param <K> Type of the cache key
/// @param <V> Type of the cached value
@SuppressWarnings("unused")
public interface Memo<K, V> {
    /// Get the cached value for the given key. If not cached, computes and caches the value.
    ///
    /// @param key The key to look up or compute
    ///
    /// @return The cached or computed value
    V get(K key);

    /// Invalidate the cached value for the given key.
    ///
    /// @param key The key to invalidate
    void invalidate(K key);

    /// Invalidate all cached values.
    void invalidateAll();

    /// Get the number of cache hits.
    ///
    /// @return The hit count
    long hitCount();

    /// Get the number of cache misses.
    ///
    /// @return The miss count
    long missCount();

    /// Get the current number of cached entries.
    ///
    /// @return The cache size
    int size();

    /// Create a memoization cache with unlimited size.
    ///
    /// @param computation The function to compute values for uncached keys
    /// @param <K>         Type of the cache key
    /// @param <V>         Type of the cached value
    ///
    /// @return A new Memo instance
    static <K, V> Memo<K, V> memo(Fn1<V, K> computation) {
        return new UnboundedMemo<>(computation);
    }

    /// Create a memoization cache with LRU eviction.
    ///
    /// @param computation The function to compute values for uncached keys
    /// @param maxSize     Maximum number of entries before eviction
    /// @param <K>         Type of the cache key
    /// @param <V>         Type of the cached value
    ///
    /// @return A new Memo instance with LRU eviction
    static <K, V> Memo<K, V> memo(Fn1<V, K> computation, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        return new LruMemo<>(computation, maxSize);
    }
}

/// Unbounded memoization cache using ConcurrentHashMap.
final class UnboundedMemo<K, V> implements Memo<K, V> {
    private final Fn1<V, K> computation;
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    UnboundedMemo(Fn1<V, K> computation) {
        this.computation = computation;
    }

    @Override
    public V get(K key) {
        var cached = cache.get(key);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        return cache.compute(key,
                             (k, existing) -> {
                                 if (existing != null) {
                                     hits.incrementAndGet();
                                     return existing;
                                 }
                                 misses.incrementAndGet();
                                 return computation.apply(k);
                             });
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

/// LRU memoization cache using synchronized LinkedHashMap.
final class LruMemo<K, V> implements Memo<K, V> {
    private final Fn1<V, K> computation;
    private final int maxSize;
    private final Map<K, V> cache;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    LruMemo(Fn1<V, K> computation, int maxSize) {
        this.computation = computation;
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }

    @Override
    public V get(K key) {
        synchronized (cache) {
            var cached = cache.get(key);
            if (cached != null) {
                hits.incrementAndGet();
                return cached;
            }
            misses.incrementAndGet();
            var value = computation.apply(key);
            cache.put(key, value);
            return value;
        }
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
