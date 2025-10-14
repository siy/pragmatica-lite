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

package org.pragmatica.http.routing;

/// Parameter type for route parameters.
public enum ParameterType {
    /// Path parameter extracted from URL segments
    PATH,

    /// Query parameter extracted from query string
    QUERY,

    /// Header parameter extracted from request headers
    HEADER,

    /// Cookie parameter extracted from Cookie header
    COOKIE,

    /// Body parameter deserialized from request body
    BODY
}
