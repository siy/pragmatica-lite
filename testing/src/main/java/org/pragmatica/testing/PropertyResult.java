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

package org.pragmatica.testing;

import org.pragmatica.lang.Option;

/// Result of running a property-based test.
public sealed interface PropertyResult {
    /// Property passed for all generated values.
    record Passed(int tries) implements PropertyResult {}

    /// Property failed for a generated value.
    record Failed(int tryNumber,
                  Object originalInput,
                  Object shrunkInput,
                  int shrinkSteps,
                  Option<Throwable> error) implements PropertyResult {}

    /// Could not generate enough valid values to test the property.
    record Exhausted(int tries, int discarded) implements PropertyResult {}
}
