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

import org.pragmatica.lang.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Zero-dependency TOML parser supporting a practical subset of TOML 1.0.
 * <p>
 * Supported features:
 * <ul>
 *   <li>Sections: {@code [section]} and {@code [section.subsection]}</li>
 *   <li>Properties: {@code key = value}</li>
 *   <li>Quoted strings: {@code "hello world"}</li>
 *   <li>Unquoted strings: {@code hello}</li>
 *   <li>Booleans: {@code true} / {@code false}</li>
 *   <li>Integers: {@code 123}, {@code -456}</li>
 *   <li>Arrays: {@code ["a", "b", "c"]}</li>
 *   <li>Comments: {@code # comment}</li>
 * </ul>
 * <p>
 * Not yet supported:
 * <ul>
 *   <li>Inline tables: {@code {key = value}}</li>
 *   <li>Multi-line strings</li>
 *   <li>Floating point numbers</li>
 *   <li>Dates and times</li>
 *   <li>Array of tables: {@code [[array]]}</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>{@code
 * TomlParser.parse(content)
 *     .onSuccess(doc -> {
 *         doc.getString("database", "host").onPresent(System.out::println);
 *         doc.getInt("server", "port").orElse(8080);
 *     })
 *     .onFailure(error -> System.err.println(error.message()));
 * }</pre>
 */
public final class TomlParser {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_.]+)]$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+)\\s*=\\s*(.+)$");

    private TomlParser() {}

    /**
     * Parse TOML content from a string.
     *
     * @param content the TOML content to parse
     * @return Result containing the parsed TomlDocument, or an error
     */
    public static Result<TomlDocument> parse(String content) {
        if (content == null || content.isBlank()) {
            return Result.success(TomlDocument.EMPTY);
        }

        Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
        sections.put("", new LinkedHashMap<>()); // Root section

        String currentSection = "";
        int lineNumber = 0;

        for (String rawLine : content.split("\n")) {
            lineNumber++;
            String line = rawLine.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Check for section header
            var sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                currentSection = sectionMatcher.group(1);
                sections.putIfAbsent(currentSection, new LinkedHashMap<>());
                continue;
            }

            // Check for key = value
            var kvMatcher = KEY_VALUE_PATTERN.matcher(line);
            if (kvMatcher.matches()) {
                String key = kvMatcher.group(1);
                String rawValue = kvMatcher.group(2).trim();

                // Strip inline comment (but not from quoted strings)
                rawValue = stripInlineComment(rawValue);

                var parseResult = parseValue(rawValue, lineNumber);
                if (parseResult.isFailure()) {
                    return parseResult.map(_ -> null);
                }

                var section = currentSection;
                parseResult.onSuccess(value -> sections.get(section).put(key, value));
                continue;
            }

            return TomlError.syntaxError(lineNumber, line).result();
        }

        return Result.success(new TomlDocument(Map.copyOf(sections)));
    }

    /**
     * Parse TOML content from a file.
     *
     * @param path the path to the TOML file
     * @return Result containing the parsed TomlDocument, or an error
     */
    public static Result<TomlDocument> parseFile(Path path) {
        try {
            return parse(Files.readString(path));
        } catch (IOException e) {
            return TomlError.fileReadFailed(path.toString(), e.getMessage()).result();
        }
    }

    private static String stripInlineComment(String value) {
        if (value.startsWith("\"")) {
            return value; // Don't strip from quoted strings
        }
        if (value.startsWith("[") && value.contains("]")) {
            // For arrays, strip comment after closing bracket
            int closingBracket = value.lastIndexOf(']');
            String afterBracket = value.substring(closingBracket + 1);
            if (afterBracket.contains("#")) {
                return value.substring(0, closingBracket + 1).trim();
            }
            return value;
        }
        int commentIndex = value.indexOf('#');
        if (commentIndex > 0) {
            return value.substring(0, commentIndex).trim();
        }
        return value;
    }

    private static Result<Object> parseValue(String value, int lineNumber) {
        // Boolean
        if ("true".equals(value)) {
            return Result.success(true);
        }
        if ("false".equals(value)) {
            return Result.success(false);
        }

        // Quoted string
        if (value.startsWith("\"")) {
            if (!value.endsWith("\"") || value.length() < 2) {
                return TomlError.unterminatedString(lineNumber).result();
            }
            return Result.success(unescapeString(value.substring(1, value.length() - 1)));
        }

        // Array
        if (value.startsWith("[")) {
            if (!value.endsWith("]")) {
                return TomlError.unterminatedArray(lineNumber).result();
            }
            return parseArray(value.substring(1, value.length() - 1), lineNumber);
        }

        // Float (including negative, with decimal point or exponent)
        if (value.matches("-?\\d+\\.\\d+([eE][+-]?\\d+)?") || value.matches("-?\\d+[eE][+-]?\\d+")) {
            try {
                return Result.success(Double.parseDouble(value));
            } catch (NumberFormatException _) {
                return TomlError.invalidValue(lineNumber, value, "float").result();
            }
        }

        // Integer (including negative)
        if (value.matches("-?\\d+")) {
            try {
                return Result.success(Long.parseLong(value));
            } catch (NumberFormatException _) {
                return TomlError.invalidValue(lineNumber, value, "integer").result();
            }
        }

        // Unquoted string (identifier-like)
        return Result.success(value);
    }

    private static Result<Object> parseArray(String content, int lineNumber) {
        List<Object> items = new ArrayList<>();
        if (content.trim().isEmpty()) {
            return Result.success(items);
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int bracketDepth = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '[' && !inQuotes) {
                bracketDepth++;
                current.append(c);
            } else if (c == ']' && !inQuotes) {
                bracketDepth--;
                current.append(c);
            } else if (c == ',' && !inQuotes && bracketDepth == 0) {
                var itemResult = parseValue(current.toString().trim(), lineNumber);
                if (itemResult.isFailure()) {
                    return itemResult;
                }
                itemResult.onSuccess(items::add);
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add last item
        String lastItem = current.toString().trim();
        if (!lastItem.isEmpty()) {
            var itemResult = parseValue(lastItem, lineNumber);
            if (itemResult.isFailure()) {
                return itemResult;
            }
            itemResult.onSuccess(items::add);
        }

        return Result.success(items);
    }

    private static String unescapeString(String s) {
        // Process character by character to handle escape sequences correctly
        var result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\\' -> { result.append('\\'); i++; }
                    case '"' -> { result.append('"'); i++; }
                    case 'n' -> { result.append('\n'); i++; }
                    case 't' -> { result.append('\t'); i++; }
                    case 'r' -> { result.append('\r'); i++; }
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
