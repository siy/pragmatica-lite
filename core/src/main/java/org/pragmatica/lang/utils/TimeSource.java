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
/// Time source abstraction for testability.
/// Allows injecting a mock time source in tests for deterministic behavior.
@FunctionalInterface
public interface TimeSource {
    /// Returns the current time in nanoseconds.
    ///
    /// @return current time in nanoseconds
    long nanoTime();

    /// Default time source using system clock.
    static TimeSource system() {
        return System::nanoTime;
    }
}
