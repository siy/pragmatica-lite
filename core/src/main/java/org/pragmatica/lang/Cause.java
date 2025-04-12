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

import java.util.stream.Stream;

/// Basic interface for failure cause types.
public interface Cause {
    /// Message associated with the failure.
    String message();

    /// The original cause (if any) of the error.
    default Option<Cause> source() {
        return Option.empty();
    }

    /// Represent cause as a failure [Result] instance.
    ///
    /// @return cause converted into [Result] with necessary type.
    default <T> Result<T> result() {
        return Result.failure(this);
    }

    /// Represent cause as a failure [Promise] instance.
    ///
    /// @return cause converted into [Promise] with necessary type.
    default <T> Promise<T> promise() {
        return Promise.failure(this);
    }

    /// Iterate over the cause chain, starting from this cause.
    ///
    /// @param action action to be applied to each cause in the chain.
    ///
    /// @return result of the last action.
    default <T> T iterate(Fn1<T, Cause> action) {
        var value = action.apply(this);

        return source().fold(() -> value, src -> src.iterate(action));
    }

    /// Stream of causes starting from this cause. Fir single cause it will be a stream of one element. For composite cause, it will be a stream of all
    /// causes stored in this cause.
    ///
    /// @return stream of causes.
    default Stream<Cause> stream() {
        return Stream.of(this);
    }
}
