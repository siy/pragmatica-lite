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

package org.pragmatica.consensus.rabia;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.io.TimeSpan;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/**
 * Configuration for the Rabia consensus engine.
 *
 * @param cleanupInterval      Interval for cleaning up old phase data
 * @param syncRetryInterval    Interval for retrying synchronization attempts
 * @param removeOlderThanPhases Number of phases to retain before cleanup
 */
public record ProtocolConfig(TimeSpan cleanupInterval,
                             TimeSpan syncRetryInterval,
                             long removeOlderThanPhases) {
    /**
     * Validates and creates a ProtocolConfig.
     */
    public static Result<ProtocolConfig> protocolConfig(TimeSpan cleanupInterval,
                                                        TimeSpan syncRetryInterval,
                                                        long removeOlderThanPhases) {
        return Result.all(validatePositive(cleanupInterval, "cleanupInterval"),
                          validatePositive(syncRetryInterval, "syncRetryInterval"),
                          validatePositive(removeOlderThanPhases, "removeOlderThanPhases"))
                     .map((cleanup, sync, phases) -> new ProtocolConfig(cleanup, sync, phases));
    }

    private static Result<TimeSpan> validatePositive(TimeSpan value, String name) {
        return value != null && value.nanos() > 0
               ? Result.success(value)
               : ConfigError.invalidTimeSpan(name)
                            .result();
    }

    private static Result<Long> validatePositive(long value, String name) {
        return value > 0
               ? Result.success(value)
               : ConfigError.invalidValue(name, value)
                            .result();
    }

    /**
     * Creates a default (production) configuration.
     */
    public static ProtocolConfig defaultConfig() {
        return new ProtocolConfig(timeSpan(60)
                                          .seconds(), timeSpan(5)
                                                              .seconds(), 100);
    }

    /**
     * Creates a test configuration with faster intervals.
     */
    public static ProtocolConfig testConfig() {
        return new ProtocolConfig(timeSpan(60)
                                          .seconds(), timeSpan(100)
                                                              .millis(), 100);
    }

    /**
     * Configuration validation errors.
     */
    public sealed interface ConfigError extends Cause {
        record InvalidTimeSpan(String field) implements ConfigError {
            @Override
            public String message() {
                return field + " must be a positive duration";
            }
        }

        record InvalidValue(String field, long value) implements ConfigError {
            @Override
            public String message() {
                return field + " must be positive, got: " + value;
            }
        }

        static ConfigError invalidTimeSpan(String field) {
            return new InvalidTimeSpan(field);
        }

        static ConfigError invalidValue(String field, long value) {
            return new InvalidValue(field, value);
        }
    }
}
