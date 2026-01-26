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

package org.pragmatica.consensus.topology;

import org.pragmatica.lang.utils.Retry.BackoffStrategy;

import static org.pragmatica.lang.io.TimeSpan.timeSpan;

/// Configuration for connection backoff and node disabling.
///
/// @param maxAttempts     Maximum failed connection attempts before disabling a node
/// @param backoffStrategy Strategy for calculating delay between connection attempts
public record BackoffConfig(int maxAttempts, BackoffStrategy backoffStrategy) {
    /// Default configuration: 4 attempts, exponential backoff with 1s initial, 60s max, factor 1.5, with jitter.
    public static final BackoffConfig DEFAULT = new BackoffConfig(4,
                                                                  BackoffStrategy.exponential()
                                                                                 .initialDelay(timeSpan(1).seconds())
                                                                                 .maxDelay(timeSpan(60).seconds())
                                                                                 .factor(1.5)
                                                                                 .withJitter());

    /// Checks if a node should be disabled based on the number of failed attempts.
    public boolean shouldDisable(int failedAttempts) {
        return failedAttempts >= maxAttempts;
    }
}
