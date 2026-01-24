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

import java.util.function.Supplier;

/// Deferred computation with memoization.
///
/// A lazy value is computed on first access and cached thereafter. The computation
/// is thread-safe using double-checked locking.
///
/// @param <T> Type of the computed value
@SuppressWarnings("unused")
public interface Lazy<T> {
    /// Get the computed value. On first call, the value is computed and cached.
    /// Subsequent calls return the cached value.
    ///
    /// @return the computed value
    T get();

    /// Check if the value has been computed.
    ///
    /// @return true if the value has been computed, false otherwise
    boolean isComputed();

    /// Transform the lazy value using the provided mapping function.
    /// The transformation is applied lazily on first access.
    ///
    /// @param fn  Mapping function
    /// @param <R> Type of the new value
    ///
    /// @return a new Lazy that computes the transformed value
    <R> Lazy<R> map(Fn1<R, ? super T> fn);

    /// Transform the lazy value using a function that returns another Lazy.
    /// Both the outer and inner computations are performed lazily.
    ///
    /// @param fn  Mapping function returning a Lazy
    /// @param <R> Type of the new value
    ///
    /// @return a new Lazy that computes the transformed value
    <R> Lazy<R> flatMap(Fn1<Lazy<R>, ? super T> fn);

    /// Create a lazy value from a supplier.
    /// The supplier is called at most once, on first access.
    ///
    /// @param supplier Source of the value
    /// @param <T>      Type of the value
    ///
    /// @return a new Lazy instance
    static <T> Lazy<T> lazy(Supplier<T> supplier) {
        return new DeferredLazy<>(supplier);
    }

    /// Create a lazy value that is already computed.
    /// Useful when a Lazy is required but the value is already available.
    ///
    /// @param value The pre-computed value
    /// @param <T>   Type of the value
    ///
    /// @return a new Lazy instance containing the value
    static <T> Lazy<T> value(T value) {
        return new EvaluatedLazy<>(value);
    }
}

/// Internal implementation of a deferred lazy value.
final class DeferredLazy<T> implements Lazy<T> {
    private final Supplier<T> supplier;
    private volatile boolean computed;
    private T value;

    DeferredLazy(Supplier<T> supplier) {
        this.supplier = supplier;
        this.computed = false;
    }

    @Override
    public T get() {
        if (!computed) {
            synchronized (this) {
                if (!computed) {
                    value = supplier.get();
                    computed = true;
                }
            }
        }
        return value;
    }

    @Override
    public boolean isComputed() {
        return computed;
    }

    @Override
    public <R> Lazy<R> map(Fn1<R, ? super T> fn) {
        return Lazy.lazy(() -> fn.apply(get()));
    }

    @Override
    public <R> Lazy<R> flatMap(Fn1<Lazy<R>, ? super T> fn) {
        return Lazy.lazy(() -> fn.apply(get())
                                 .get());
    }

    @Override
    public String toString() {
        return computed
               ? "Lazy(" + value + ")"
               : "Lazy(<not computed>)";
    }
}

/// Internal implementation of a pre-computed lazy value.
final class EvaluatedLazy<T> implements Lazy<T> {
    private final T value;

    EvaluatedLazy(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public boolean isComputed() {
        return true;
    }

    @Override
    public <R> Lazy<R> map(Fn1<R, ? super T> fn) {
        return Lazy.lazy(() -> fn.apply(value));
    }

    @Override
    public <R> Lazy<R> flatMap(Fn1<Lazy<R>, ? super T> fn) {
        return Lazy.lazy(() -> fn.apply(value)
                                 .get());
    }

    @Override
    public String toString() {
        return "Lazy(" + value + ")";
    }
}
