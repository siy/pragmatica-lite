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

package org.pragmatica.r2dbc;

import org.pragmatica.lang.Functions.Fn1;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/// Bridge utilities for converting Reactive Streams Publisher to Promise.
/// Provides seamless integration between reactive and promise-based APIs.
public interface ReactiveOperations {
    /// Converts a Publisher emitting a single value to a Promise.
    /// Fails if the publisher emits zero or more than one value.
    ///
    /// @param publisher Publisher to convert
    /// @param errorMapper Function to map exceptions to errors
    /// @param <T> Element type
    ///
    /// @return Promise containing the single emitted value
    @SuppressWarnings("unchecked")
    static <T> Promise<T> fromPublisher(Publisher<? extends T> publisher, Fn1<R2dbcError, Throwable> errorMapper) {
        return Promise.promise(promise -> {
                                   var valueRef = new AtomicReference<T>();
                                   var countRef = new AtomicInteger(0);
                                   ((Publisher<T>) publisher).subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                                                                            subscription = s;
                                                                            s.request(2);
                                                                        }

            @Override
            public void onNext(T item) {
                                                                            var count = countRef.incrementAndGet();
                                                                            if (count == 1) {
                                                                                valueRef.set(item);
                                                                            } else if (count == 2) {
                                                                                subscription.cancel();
                                                                                promise.resolve(new R2dbcError.MultipleResults(count).result());
                                                                            }
                                                                        }

            @Override
            public void onError(Throwable t) {
                                                                            promise.resolve(errorMapper.apply(t)
                                                                                                       .result());
                                                                        }

            @Override
            public void onComplete() {
                                                                            var count = countRef.get();
                                                                            if (count == 0) {
                                                                                promise.resolve(R2dbcError.NoResult.INSTANCE.result());
                                                                            } else if (count == 1) {
                                                                                promise.resolve(Result.success(valueRef.get()));
                                                                            }
                                                                        }
        });
                               });
    }

    /// Converts a Publisher to a Promise, returning the first value if present.
    ///
    /// @param publisher Publisher to convert
    /// @param errorMapper Function to map exceptions to errors
    /// @param <T> Element type
    ///
    /// @return Promise containing Option with the first emitted value
    @SuppressWarnings("unchecked")
    static <T> Promise<Option<T>> firstFromPublisher(Publisher<? extends T> publisher,
                                                     Fn1<R2dbcError, Throwable> errorMapper) {
        return Promise.promise(promise -> {
                                   ((Publisher<T>) publisher).subscribe(new Subscriber<T>() {
            private Subscription subscription;
            private boolean completed = false;

            @Override
            public void onSubscribe(Subscription s) {
                                                                            subscription = s;
                                                                            s.request(1);
                                                                        }

            @Override
            public void onNext(T item) {
                                                                            if (!completed) {
                                                                                completed = true;
                                                                                subscription.cancel();
                                                                                promise.resolve(Result.success(Option.option(item)));
                                                                            }
                                                                        }

            @Override
            public void onError(Throwable t) {
                                                                            if (!completed) {
                                                                                promise.resolve(errorMapper.apply(t)
                                                                                                           .result());
                                                                            }
                                                                        }

            @Override
            public void onComplete() {
                                                                            if (!completed) {
                                                                                promise.resolve(Result.success(Option.none()));
                                                                            }
                                                                        }
        });
                               });
    }

    /// Collects all values from a Publisher into a List.
    ///
    /// @param publisher Publisher to convert
    /// @param errorMapper Function to map exceptions to errors
    /// @param <T> Element type
    ///
    /// @return Promise containing list of all emitted values
    @SuppressWarnings("unchecked")
    static <T> Promise<List<T>> collectFromPublisher(Publisher<? extends T> publisher,
                                                     Fn1<R2dbcError, Throwable> errorMapper) {
        return Promise.promise(promise -> {
                                   var results = new ArrayList<T>();
                                   ((Publisher<T>) publisher).subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                                                                            s.request(Long.MAX_VALUE);
                                                                        }

            @Override
            public void onNext(T item) {
                                                                            results.add(item);
                                                                        }

            @Override
            public void onError(Throwable t) {
                                                                            promise.resolve(errorMapper.apply(t)
                                                                                                       .result());
                                                                        }

            @Override
            public void onComplete() {
                                                                            promise.resolve(Result.success(results));
                                                                        }
        });
                               });
    }

    /// Convenience method with default error mapping.
    static <T> Promise<T> fromPublisher(Publisher<? extends T> publisher) {
        return fromPublisher(publisher, R2dbcError::fromException);
    }

    /// Convenience method with default error mapping.
    static <T> Promise<Option<T>> firstFromPublisher(Publisher<? extends T> publisher) {
        return firstFromPublisher(publisher, R2dbcError::fromException);
    }

    /// Convenience method with default error mapping.
    static <T> Promise<List<T>> collectFromPublisher(Publisher<? extends T> publisher) {
        return collectFromPublisher(publisher, R2dbcError::fromException);
    }
}
