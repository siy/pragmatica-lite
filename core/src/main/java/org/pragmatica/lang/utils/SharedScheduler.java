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

import org.pragmatica.lang.io.TimeSpan;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/// Common scheduler for use by [Retry] and [CircuitBreaker]
public final class SharedScheduler {
    private SharedScheduler() {}

    private static final ScheduledExecutorService SCHEDULER = new ScheduledThreadPoolExecutor(2);

    /// Schedule one-time invocation
    public static void schedule(Runnable runnable, TimeSpan interval) {
        SCHEDULER.schedule(runnable, interval.millis(), TimeUnit.MILLISECONDS);
    }

    /// Schedule periodic invocation
    public static void scheduleAtFixedRate(Runnable runnable, TimeSpan interval) {
        SCHEDULER.scheduleAtFixedRate(runnable, interval.millis(), interval.millis(), TimeUnit.MILLISECONDS);
    }
}
