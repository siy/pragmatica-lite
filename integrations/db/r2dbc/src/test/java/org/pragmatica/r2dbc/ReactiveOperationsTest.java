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

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

class ReactiveOperationsTest {

    @Test
    void fromPublisher_returnsSingleValue() {
        Publisher<String> publisher = singleValuePublisher("hello");

        ReactiveOperations.fromPublisher(publisher)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(value -> assertThat(value).isEqualTo("hello"));
    }

    @Test
    void fromPublisher_failsOnEmpty() {
        Publisher<String> publisher = emptyPublisher();

        ReactiveOperations.fromPublisher(publisher)
            .await()
            .onSuccess(_ -> fail("Expected failure"))
            .onFailure(cause -> assertInstanceOf(R2dbcError.NoResult.class, cause));
    }

    @Test
    void fromPublisher_failsOnMultiple() {
        Publisher<String> publisher = multiValuePublisher(List.of("a", "b", "c"));

        ReactiveOperations.fromPublisher(publisher)
            .await()
            .onSuccess(_ -> fail("Expected failure"))
            .onFailure(cause -> assertInstanceOf(R2dbcError.MultipleResults.class, cause));
    }

    @Test
    void firstFromPublisher_returnsSome() {
        Publisher<String> publisher = multiValuePublisher(List.of("first", "second"));

        ReactiveOperations.firstFromPublisher(publisher)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(opt -> {
                assertThat(opt.isPresent()).isTrue();
                assertThat(opt.or("")).isEqualTo("first");
            });
    }

    @Test
    void firstFromPublisher_returnsNone() {
        Publisher<String> publisher = emptyPublisher();

        ReactiveOperations.firstFromPublisher(publisher)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(opt -> assertThat(opt.isEmpty()).isTrue());
    }

    @Test
    void collectFromPublisher_returnsAll() {
        Publisher<String> publisher = multiValuePublisher(List.of("a", "b", "c"));

        ReactiveOperations.collectFromPublisher(publisher)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(list -> assertThat(list).containsExactly("a", "b", "c"));
    }

    @Test
    void collectFromPublisher_returnsEmptyList() {
        Publisher<String> publisher = emptyPublisher();

        ReactiveOperations.collectFromPublisher(publisher)
            .await()
            .onFailure(_ -> fail("Expected success"))
            .onSuccess(list -> assertThat(list).isEmpty());
    }

    @Test
    void fromPublisher_handlesError() {
        Publisher<String> publisher = errorPublisher(new RuntimeException("Test error"));

        ReactiveOperations.fromPublisher(publisher)
            .await()
            .onSuccess(_ -> fail("Expected failure"))
            .onFailure(cause -> assertThat(cause.message()).contains("Test error"));
    }

    private <T> Publisher<T> singleValuePublisher(T value) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean completed = false;

            @Override
            public void request(long n) {
                if (!completed && n > 0) {
                    completed = true;
                    subscriber.onNext(value);
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                completed = true;
            }
        });
    }

    private <T> Publisher<T> emptyPublisher() {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                subscriber.onComplete();
            }

            @Override
            public void cancel() {}
        });
    }

    private <T> Publisher<T> multiValuePublisher(List<T> values) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private int index = 0;
            private boolean cancelled = false;

            @Override
            public void request(long n) {
                while (n > 0 && index < values.size() && !cancelled) {
                    subscriber.onNext(values.get(index++));
                    n--;
                }
                if (index >= values.size() && !cancelled) {
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        });
    }

    private <T> Publisher<T> errorPublisher(Throwable error) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                subscriber.onError(error);
            }

            @Override
            public void cancel() {}
        });
    }
}
