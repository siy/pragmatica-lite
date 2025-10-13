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

import org.pragmatica.lang.Option;

import java.util.List;
import java.util.Map;

/// Immutable collection of query parameters.
/// Each parameter can have multiple values.
public record QueryParameters(Map<String, List<String>> parameters) {
    public QueryParameters {
        parameters = Map.copyOf(parameters);
    }

    /// Get a single query parameter value by name.
    /// If multiple values exist, returns the first one.
    ///
    /// @param name parameter name
    /// @return Option containing parameter value if present
    public Option<String> get(String name) {
        return Option.option(parameters.get(name))
            .flatMap(list -> Option.option(list.isEmpty() ? null : list.get(0)));
    }

    /// Get all query parameter values by name.
    ///
    /// @param name parameter name
    /// @return Option containing list of values if present
    public Option<List<String>> getAll(String name) {
        return Option.option(parameters.get(name));
    }

    /// Create an empty query parameters collection.
    ///
    /// @return empty QueryParameters
    public static QueryParameters empty() {
        return new QueryParameters(Map.of());
    }

    /// Create query parameters from a map.
    ///
    /// @param params map of parameter names to value lists
    /// @return QueryParameters instance
    public static QueryParameters fromMap(Map<String, List<String>> params) {
        return new QueryParameters(params);
    }
}
