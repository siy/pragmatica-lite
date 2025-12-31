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

package org.pragmatica.utility;
/**
 * ID generator using KSUID for unique, time-sortable identifiers.
 */
public sealed interface IdGenerator {
    /// Generate a unique ID with the given prefix.
    ///
    /// @param prefix the prefix for the ID
    /// @return a unique ID in the format "prefix-ksuid"
    static String generate(String prefix) {
        return prefix + "-" + KSUID.ksuid()
                                  .encoded();
    }

    @SuppressWarnings("unused")
    record unused() implements IdGenerator {}
}
