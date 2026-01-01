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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP headers with case-insensitive lookup (per HTTP specification).
 */
public interface Headers {
    /**
     * Get the first value for a header (case-insensitive lookup).
     */
    Option<String> get(String name);

    /**
     * Get all values for a header (case-insensitive lookup).
     */
    List<String> getAll(String name);

    /**
     * Get all headers as a map (keys are lowercase).
     */
    Map<String, List<String>> asMap();

    /**
     * Create headers from a raw map (will be normalized to lowercase keys).
     */
    static Headers headers(Map<String, List<String>> raw) {
        record headers(Map<String, List<String>> normalized) implements Headers {
            @Override
            public Option<String> get(String name) {
                var values = normalized.get(name.toLowerCase());
                return values == null || values.isEmpty()
                       ? Option.empty()
                       : Option.some(values.getFirst());
            }

            @Override
            public List<String> getAll(String name) {
                var values = normalized.get(name.toLowerCase());
                return values == null
                       ? List.of()
                       : values;
            }

            @Override
            public Map<String, List<String>> asMap() {
                return normalized;
            }
        }
        var normalized = new HashMap<String, List<String>>();
        raw.forEach((k, v) -> normalized.put(k.toLowerCase(), List.copyOf(v)));
        return new headers(Map.copyOf(normalized));
    }

    /**
     * Create headers from a single-value map (convenience for simple cases).
     */
    static Headers fromSingleValueMap(Map<String, String> raw) {
        var multi = new HashMap<String, List<String>>();
        raw.forEach((k, v) -> multi.put(k.toLowerCase(), List.of(v)));
        return headers(multi);
    }

    /**
     * Empty headers.
     */
    static Headers empty() {
        return headers(Map.of());
    }
}
