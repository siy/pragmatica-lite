/*
 *  Copyright (c) 2022-2025 Sergiy Yevtushenko.
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

package org.pragmatica.net.dns;

import org.pragmatica.lang.Cause;

/// DNS resolution errors.
public sealed interface ResolverError extends Cause {
    record InvalidIpAddress(String message) implements ResolverError {}

    record ServerError(String message) implements ResolverError {}

    record RequestTimeout(String message) implements ResolverError {}

    record UnknownError(String message) implements ResolverError {}

    record UnknownDomain(String message) implements ResolverError {}
}
