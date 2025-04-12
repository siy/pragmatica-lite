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
 *
 */

package org.pragmatica.lang.io;

import org.pragmatica.lang.Tuple.Tuple2;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.pragmatica.lang.Tuple.tuple;

/// Representation of time span.
public sealed interface TimeSpan extends Comparable<TimeSpan> {
    /// Time span value represented as number of nanoseconds.
    ///
    /// @return time span in nanoseconds
    long nanos();

    /// Time span value represented as number of microseconds.
    ///
    /// @return time span in microseconds
    default long micros() {
        return TimeUnit.NANOSECONDS.toMicros(nanos());
    }

    /// Time span value represented as number of milliseconds.
    ///
    /// @return time span in milliseconds
    default long millis() {
        return TimeUnit.NANOSECONDS.toMillis(nanos());
    }

    /// Create time span as a sum of current time span and provided argument.
    default TimeSpan plus(TimeSpan other) {
        return new TimeSpanImpl(nanos() + other.nanos());
    }

    /// Create time span as a sum of current time span and provided argument.
    default TimeSpan plus(long value) {
        return plus(value, TimeUnit.NANOSECONDS);
    }

    /// Create time span as a sum of current time span and provided argument.
    default TimeSpan plus(long value, TimeUnit unit) {
        return new TimeSpanImpl(nanos() + unit.toNanos(value));
    }

    long NANOS_IN_SECOND = TimeUnit.SECONDS.toNanos(1);
    long MILLIS_IN_SECOND = TimeUnit.MILLISECONDS.toNanos(1);

    /// Time span value represented as number of whole seconds and remaining nanoseconds. This representation is compatible with many use cases, for
    /// example with [Duration] (see [#duration()]).
    ///
    /// @return time span represented as tuple containing number of seconds and remaining nanoseconds
    default Tuple2<Long, Integer> secondsAndNanos() {
        return tuple(nanos() / NANOS_IN_SECOND, (int) (nanos() % NANOS_IN_SECOND));
    }

    /// Time span value represented as number of whole milliseconds and remaining nanoseconds. This representation is compatible with many use cases, for
    /// example with [span represented as tuple containing number of milliseconds and remaining nanoseconds][#sleep(long,int).time]
    default Tuple2<Long, Integer> millisAndNanos() {
        return tuple(nanos() / MILLIS_IN_SECOND, (int) (nanos() % MILLIS_IN_SECOND));
    }

    /// Time span value represented as [Duration].
    ///
    /// @return time span as [Duration]
    default Duration duration() {
        return secondsAndNanos().map(Duration::ofSeconds);
    }

    @Override
    default int compareTo(TimeSpan o) {
        return Long.compare(nanos(), o.nanos());
    }

    static TimeSpan fromDuration(Duration ttl) {
        return TimeSpan.timeSpan(ttl.toMillis()).millis();
    }

    /// Create instance of time span builder.
    ///
    /// @param value initial value passed to builder.
    ///
    /// @return builder instance
    static TimeSpanBuilder timeSpan(long value) {
        return () -> value;
    }

    record TimeSpanImpl(long nanos) implements TimeSpan {
        @Override
        public String toString() {
            return "TimeSpan(" + duration().toString().substring(2) + ")";
        }
    }

    /// Fluent interval conversion builder
    interface TimeSpanBuilder {
        long value();

        /// Create [TimeSpan] instance by interpreting value as nanoseconds.
        ///
        /// @return Created instance
        default TimeSpan nanos() {
            return new TimeSpanImpl(value());
        }

        /// Create [TimeSpan] instance by interpreting value as microseconds.
        ///
        /// @return Created instance
        default TimeSpan micros() {
            return new TimeSpanImpl(TimeUnit.MICROSECONDS.toNanos(value()));
        }

        /// Create [TimeSpan] instance by interpreting value as milliseconds.
        ///
        /// @return Created instance
        default TimeSpan millis() {
            return new TimeSpanImpl(TimeUnit.MILLISECONDS.toNanos(value()));
        }

        /// Create [TimeSpan] instance by interpreting value as seconds.
        ///
        /// @return Created instance
        default TimeSpan seconds() {
            return new TimeSpanImpl(TimeUnit.SECONDS.toNanos(value()));
        }

        /// Create [TimeSpan] instance by interpreting value as minutes.
        ///
        /// @return Created instance
        default TimeSpan minutes() {
            return new TimeSpanImpl(TimeUnit.MINUTES.toNanos(value()));
        }

        /// Create [TimeSpan] instance by interpreting value as hours.
        ///
        /// @return Created instance
        default TimeSpan hours() {
            return new TimeSpanImpl(TimeUnit.HOURS.toNanos(value()));
        }

        /// Create [TimeSpan] instance by interpreting value as days.
        ///
        /// @return Created instance
        default TimeSpan days() {
            return new TimeSpanImpl(TimeUnit.DAYS.toNanos(value()));
        }
    }
}
