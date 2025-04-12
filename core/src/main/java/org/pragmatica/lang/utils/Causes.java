/*
 *  Copyright (c) 2023-2025 Sergiy Yevtushenko.
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
import org.pragmatica.lang.Option;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;

/// Frequently used variants of [Cause].
@SuppressWarnings("unused")
public sealed interface Causes {
    record unused() implements Causes {}

    /// Simplest possible variant of [Cause] which contains only message describing the cause
    interface SimpleCause extends Cause {
        default String completeMessage() {
            var builder = new StringBuilder("Cause: ").append(message());

            iterate(issue -> builder.append("\n  ").append(issue.message()));

            return builder.toString();
        }
    }

    /// Construct a simple cause with a given message.
    ///
    /// @param message message describing the cause
    /// @return created instance
    static Cause cause(String message) {
        return cause(message, none());
    }

    static Cause cause(String message, Option<Cause> source) {
        record simpleCause(String message, Option<Cause> source) implements SimpleCause {
            @Override
            public String toString() {
                return completeMessage();
            }
        }

        return new simpleCause(message, source);
    }

    /// Construct a simple cause from provided [Throwable].
    ///
    /// @param throwable the instance of [Throwable] to extract stack trace and message from
    /// @return created instance
    static Cause fromThrowable(Throwable throwable) {
        var sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));

        return cause(sw.toString());
    }

    /// Create a mapper which will map a value into a formatted message. Main use case for this function - creation of mappers for
    /// [#filter(Fn1,Predicate)]:
    /// <blockquote><pre>
    /// filter(Causes.forValue("Value {0} is below threshold"), value -> value > 321)
    /// </pre></blockquote>
    ///
    /// @param template the message template prepared for [MessageFormat]
    /// @return created mapping function
    static <T> Fn1<Cause, T> forValue(String template) {
        return (T input) -> cause(String.format(template, input));
    }

    interface CompositeCause extends Cause {
        static Cause toComposite(String text, Cause cause) {
            if (cause instanceof CompositeCause composite) {
                return composite.append(cause(text));
            }
            return composite().append(cause(text, option(cause)));
        }

        CompositeCause append(Cause cause);

        boolean isEmpty();
    }

    static CompositeCause composite() {
        record compositeCause(Option<Cause> source, List<Cause> causes) implements CompositeCause {
            @Override
            public CompositeCause append(Cause cause) {
                causes().add(cause);
                return this;
            }

            @Override
            public Stream<Cause> stream() {
                return causes().stream();
            }

            @Override
            public boolean isEmpty() {
                return causes().isEmpty();
            }

            @Override
            public String message() {
                var builder = new StringBuilder("Composite:");

                stream().forEach(issue -> builder.append("\n  ")
                                                 .append(issue.message()));
                return builder.toString();
            }

            @Override
            public String toString() {
                return message();
            }
        }

        return new compositeCause(none(), new ArrayList<>());
    }
}
