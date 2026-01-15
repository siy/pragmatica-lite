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
 */

package org.pragmatica.config.toml;

import org.pragmatica.lang.Option;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Immutable TOML document providing typed access to configuration values.
///
/// Values are accessed via fluent API that returns [Option] for nullable results:
/// <pre>{@code
/// document.getString("database", "host")     // Option<String>
/// document.getInt("server", "port")          // Option<Integer>
/// document.getBoolean("features", "enabled") // Option<Boolean>
/// document.getStringList("tags", "values")   // Option<List<String>>
/// }</pre>
///
/// Root-level properties use empty string as section:
/// <pre>{@code
/// document.getString("", "title")  // Root-level 'title' property
/// }</pre>
///
/// Array of tables (TOML `[[section]]` syntax) are accessed via:
/// <pre>{@code
/// document.getTableArray("products")  // Option<List<Map<String, Object>>>
/// }</pre>
///
/// @param sections     Map of section names to their key-value pairs
/// @param tableArrays  Map of array table names to list of table maps
public record TomlDocument(Map<String, Map<String, Object>> sections,
                           Map<String, List<Map<String, Object>>> tableArrays) {
    /// Empty document constant.
    public static final TomlDocument EMPTY = new TomlDocument(Map.of("", Map.of()),
                                                              Map.of());

    /// Canonical constructor ensuring immutable storage.
    public TomlDocument {
        sections = Map.copyOf(sections);
        tableArrays = Map.copyOf(tableArrays);
    }

    /// Compatibility constructor for documents without array tables.
    public TomlDocument(Map<String, Map<String, Object>> sections) {
        this(sections, Map.of());
    }

    /// Get a string value from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the string value, or empty if not found
    public Option<String> getString(String section, String key) {
        return getValue(section, key)
                       .map(Object::toString);
    }

    /// Get an integer value from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the integer value, or empty if not found or not an integer
    public Option<Integer> getInt(String section, String key) {
        return getValue(section, key)
                       .flatMap(this::toInt);
    }

    /// Get a long value from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the long value, or empty if not found or not a number
    public Option<Long> getLong(String section, String key) {
        return getValue(section, key)
                       .flatMap(this::toLong);
    }

    /// Get a double value from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the double value, or empty if not found or not a number
    public Option<Double> getDouble(String section, String key) {
        return getValue(section, key)
                       .flatMap(this::toDouble);
    }

    /// Get a boolean value from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the boolean value, or empty if not found or not a boolean
    public Option<Boolean> getBoolean(String section, String key) {
        return getValue(section, key)
                       .flatMap(this::toBoolean);
    }

    /// Get a string list from the document.
    ///
    /// @param section the section name (empty string for root)
    /// @param key     the property key
    /// @return Option containing the string list, or empty if not found or not a list
    public Option<List<String>> getStringList(String section, String key) {
        return getValue(section, key)
                       .flatMap(this::toStringList);
    }

    /// Get all keys in a section.
    ///
    /// @param section the section name (empty string for root)
    /// @return Set of keys in the section, or empty set if section not found
    public Set<String> keys(String section) {
        return Option.option(sections.get(section))
                     .map(Map::keySet)
                     .or(Set.of());
    }

    /// Get all section names in the document.
    ///
    /// @return Set of section names (includes empty string for root if it has properties)
    public Set<String> sectionNames() {
        return sections.keySet();
    }

    /// Check if a section exists.
    ///
    /// @param section the section name
    /// @return true if the section exists
    public boolean hasSection(String section) {
        return sections.containsKey(section);
    }

    /// Check if a key exists in a section.
    ///
    /// @param section the section name
    /// @param key     the property key
    /// @return true if the key exists in the section
    public boolean hasKey(String section, String key) {
        return Option.option(sections.get(section))
                     .map(m -> m.containsKey(key))
                     .or(false);
    }

    /// Get all key-value pairs from a section as strings.
    ///
    /// @param section the section name
    /// @return Map of key-value pairs, or empty map if section not found
    public Map<String, String> getSection(String section) {
        return Option.option(sections.get(section))
                     .map(this::toStringMap)
                     .or(Map.of());
    }

    /// Create a new document with an additional or updated value.
    ///
    /// @param section the section name
    /// @param key     the property key
    /// @param value   the value to set
    /// @return new TomlDocument with the value set
    public TomlDocument with(String section, String key, Object value) {
        var newSections = new LinkedHashMap<>(sections);
        var sectionMap = new LinkedHashMap<>(newSections.getOrDefault(section, Map.of()));
        sectionMap.put(key, value);
        newSections.put(section, sectionMap);
        return new TomlDocument(Map.copyOf(newSections), tableArrays);
    }

    /// Get an array of tables by name.
    ///
    /// Each `[[name]]` occurrence in TOML creates a new table in the array.
    ///
    /// @param name the array table name
    /// @return Option containing list of table maps, or empty if not found
    public Option<List<Map<String, Object>>> getTableArray(String name) {
        return Option.option(tableArrays.get(name));
    }

    /// Check if an array of tables exists.
    ///
    /// @param name the array table name
    /// @return true if the array of tables exists
    public boolean hasTableArray(String name) {
        return tableArrays.containsKey(name);
    }

    /// Get all array table names in the document.
    ///
    /// @return Set of array table names
    public Set<String> tableArrayNames() {
        return tableArrays.keySet();
    }

    private Option<Object> getValue(String section, String key) {
        return Option.option(sections.get(section))
                     .flatMap(m -> Option.option(m.get(key)));
    }

    private Option<Integer> toInt(Object value) {
        if (value instanceof Integer i) {
            return Option.some(i);
        }
        if (value instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return Option.some(l.intValue());
        }
        if (value instanceof String s) {
            try{
                return Option.some(Integer.parseInt(s));
            } catch (NumberFormatException _) {
                return Option.none();
            }
        }
        return Option.none();
    }

    private Option<Long> toLong(Object value) {
        if (value instanceof Long l) {
            return Option.some(l);
        }
        if (value instanceof Integer i) {
            return Option.some(i.longValue());
        }
        if (value instanceof String s) {
            try{
                return Option.some(Long.parseLong(s));
            } catch (NumberFormatException _) {
                return Option.none();
            }
        }
        return Option.none();
    }

    private Option<Double> toDouble(Object value) {
        if (value instanceof Double d) {
            return Option.some(d);
        }
        if (value instanceof Long l) {
            return Option.some(l.doubleValue());
        }
        if (value instanceof Integer i) {
            return Option.some(i.doubleValue());
        }
        if (value instanceof String s) {
            try{
                return Option.some(Double.parseDouble(s));
            } catch (NumberFormatException _) {
                return Option.none();
            }
        }
        return Option.none();
    }

    private Option<Boolean> toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return Option.some(b);
        }
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s)) {
                return Option.some(true);
            }
            if ("false".equalsIgnoreCase(s)) {
                return Option.some(false);
            }
        }
        return Option.none();
    }

    private Option<List<String>> toStringList(Object value) {
        if (value instanceof List< ?> list) {
            return Option.some(list.stream()
                                   .map(Object::toString)
                                   .toList());
        }
        return Option.none();
    }

    private Map<String, String> toStringMap(Map<String, Object> map) {
        var result = new LinkedHashMap<String, String>();
        map.forEach((k, v) -> result.put(k, v.toString()));
        return Map.copyOf(result);
    }
}
