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

package org.pragmatica.http;

import org.pragmatica.lang.Option;

import java.util.List;
import java.util.Map;

/// HTTP query parameters.
public interface QueryParams {
    /// Get the first value for a parameter.
    Option<String> get(String name);

    /// Get all values for a parameter.
    List<String> getAll(String name);

    /// Get all parameters as a map.
    Map<String, List<String>> asMap();

    /// Check if parameter exists.
    default boolean has(String name) {
        return get(name).isPresent();
    }

    /// Create query parameters from a map.
    static QueryParams queryParams(Map<String, List<String>> raw) {
        record queryParams(Map<String, List<String>> params) implements QueryParams {
            @Override
            public Option<String> get(String name) {
                var values = params.get(name);
                return values == null || values.isEmpty()
                       ? Option.empty()
                       : Option.some(values.getFirst());
            }

            @Override
            public List<String> getAll(String name) {
                var values = params.get(name);
                return values == null
                       ? List.of()
                       : values;
            }

            @Override
            public Map<String, List<String>> asMap() {
                return params;
            }
        }
        return new queryParams(Map.copyOf(raw));
    }

    /// Empty query parameters.
    static QueryParams empty() {
        return queryParams(Map.of());
    }
}
