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

import java.util.function.Consumer;
import java.util.function.Predicate;

/// Predicate which supports fluent API
public interface FluentPredicate<T> extends Predicate<T> {
    default FluentPredicate<T> ifTrue(T value, Consumer<T> consumer) {
        if (test(value)) {
            consumer.accept(value);
        }
        return this;
    }

    default FluentPredicate<T> ifTrue(T value, Runnable runnable) {
        return ifTrue(value, _ -> runnable.run());
    }

    default FluentPredicate<T> ifFalse(T value, Consumer<T> consumer) {
        if (!test(value)) {
            consumer.accept(value);
        }
        return this;
    }

    default FluentPredicate<T> ifFalse(T value, Runnable runnable) {
        return ifFalse(value, _ -> runnable.run());
    }

    static <T> FluentPredicate<T> from(Predicate<T> predicate) {
        return predicate::test;
    }
}
