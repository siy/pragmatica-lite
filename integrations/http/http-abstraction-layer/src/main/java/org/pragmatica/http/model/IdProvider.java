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

package org.pragmatica.http.model;

import java.util.UUID;

/// Provider for generating unique request identifiers.
/// Used by RequestFactory to assign IDs to incoming requests.
@FunctionalInterface
public interface IdProvider {
    /// Generate a unique request ID.
    ///
    /// @return unique identifier string
    String generateId();

    /// Default implementation using UUID.
    ///
    /// @return IdProvider that generates UUID-based IDs
    static IdProvider uuid() {
        return () -> "req_" + UUID.randomUUID();
    }
}
