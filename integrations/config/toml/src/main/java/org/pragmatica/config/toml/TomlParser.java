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

/// Zero-dependency TOML parser supporting a practical subset of TOML 1.0.
///
/// Supported features:
///
///   - Sections: `[section]` and `[section.subsection]`
///   - Properties: `key = value`
///   - Quoted strings: `"hello world"`
///   - Multi-line basic strings: `"""..."""` with escape sequences
///   - Multi-line literal strings: `'''...'''` without escape processing
///   - Unquoted strings: `hello`
///   - Booleans: `true` / `false`
///   - Integers: `123`, `-456`
///   - Floating point numbers: `3.14`, `-0.5e10`
///   - Arrays: `["a", "b", "c"]`
///   - Comments: `# comment`
///
/// Not yet supported:
///
///   - Inline tables: `{key = value}`
///   - Dates and times
///   - Array of tables: `[[array]]`
///
/// Example usage:
/// <pre>{@code
/// TomlParser.parse(content)
///     .onSuccess(doc -> {
///         doc.getString("database", "host").onPresent(System.out::println);
///         doc.getInt("server", "port").orElse(8080);
///     })
///     .onFailure(error -> System.err.println(error.message()));
/// }</pre>
public final class TomlParser {
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_.\\-]+)]$");
    private static final Pattern BARE_KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+)\\s*=\\s*(.+)$");
    private static final Pattern QUOTED_KEY_VALUE_PATTERN = Pattern.compile("^\"([^\"]+)\"\\s*=\\s*(.+)$");
    private static final Pattern LITERAL_KEY_VALUE_PATTERN = Pattern.compile("^'([^']+)'\\s*=\\s*(.+)$");
    private static final Pattern DOTTED_KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_.\\-]+)\\s*=\\s*(.+)$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("-?\\d+\\.\\d+([eE][+-]?\\d+)?");
    private static final Pattern FLOAT_EXP_PATTERN = Pattern.compile("-?\\d+[eE][+-]?\\d+");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+");

    private TomlParser() {}

    /// Encapsulates state for parsing multiline constructs (strings and arrays).
    private record MultilineState(String pendingKey,
                                  String pendingSection,
                                  StringBuilder builder,
                                  boolean isLiteral,
                                  boolean isArray,
                                  int startLine) {
        static MultilineState forString(String key,
                                        String section,
                                        String initial,
                                        boolean literal,
                                        int line) {
            return new MultilineState(key, section, new StringBuilder(initial), literal, false, line);
        }

        static MultilineState forArray(String key, String section, String initial, int line) {
            return new MultilineState(key, section, new StringBuilder(initial), false, true, line);
        }

        void append(String s) {
            builder.append(s);
        }

        void appendLine(String s) {
            builder.append(s)
                   .append("\n");
        }

        String content() {
            return builder.toString();
        }
    }

    /// Parse TOML content from a string.
    ///
    /// @param content the TOML content to parse
    /// @return Result containing the parsed TomlDocument, or an error
    public static Result<TomlDocument> parse(String content) {
        if (content == null || content.isBlank()) {
            return Result.success(TomlDocument.EMPTY);
        }
        Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
        sections.put("", new LinkedHashMap<>());
        String currentSection = "";
        int lineNumber = 0;
        MultilineState multiline = null;
        // -1 keeps trailing empty strings
        String[] lines = content.split("\n", - 1);
        for (String rawLine : lines) {
            lineNumber++ ;
            // Handle multiline array continuation
            if (multiline != null && multiline.isArray()) {
                var result = handleMultilineArrayContinuation(multiline, rawLine, lineNumber, sections);
                if (result.isFailure()) {
                    return result.map(_ -> null);
                }
                if (result.fold(_ -> false, completed -> completed)) {
                    multiline = null;
                }
                continue;
            }
            // Handle multiline string continuation
            if (multiline != null) {
                String delimiter = multiline.isLiteral()
                                   ? "'''"
                                   : "\"\"\"";
                int endIndex = rawLine.indexOf(delimiter);
                if (endIndex >= 0) {
                    multiline.append(rawLine.substring(0, endIndex));
                    String processed = multiline.isLiteral()
                                       ? processLiteralMultiline(multiline.content())
                                       : processBasicMultiline(multiline.content());
                    sections.get(multiline.pendingSection())
                            .put(multiline.pendingKey(),
                                 processed);
                    multiline = null;
                } else {
                    multiline.appendLine(rawLine);
                }
                continue;
            }
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
            // Try to parse key = value with different key formats
            var keyValueResult = parseKeyValue(line);
            if (keyValueResult != null) {
                String key = keyValueResult.key();
                String targetSection = keyValueResult.targetSection();
                String rawValue = keyValueResult.value();
                // Handle dotted keys - create implicit sections
                String effectiveSection = currentSection;
                String effectiveKey = key;
                if (targetSection != null) {
                    effectiveSection = currentSection.isEmpty()
                                       ? targetSection
                                       : currentSection + "." + targetSection;
                    sections.putIfAbsent(effectiveSection, new LinkedHashMap<>());
                }
                // Check for duplicate key
                if (sections.get(effectiveSection)
                            .containsKey(effectiveKey)) {
                    return TomlError.duplicateKey(lineNumber, effectiveKey)
                                    .result();
                }
                // Check for multiline string start
                if (rawValue.startsWith("\"\"\"")) {
                    var result = handleMultilineStart(rawValue, "\"\"\"", false, lineNumber);
                    switch (result) {
                        case MultilineStartResult.Complete c ->
                        sections.get(effectiveSection)
                                .put(effectiveKey,
                                     c.value());
                        case MultilineStartResult.Error e ->
                        {
                            return e.error();
                        }
                        case MultilineStartResult.Partial p ->
                        multiline = MultilineState.forString(effectiveKey,
                                                             effectiveSection,
                                                             p.partial(),
                                                             false,
                                                             lineNumber);
                    }
                    continue;
                }
                if (rawValue.startsWith("'''")) {
                    var result = handleMultilineStart(rawValue, "'''", true, lineNumber);
                    switch (result) {
                        case MultilineStartResult.Complete c ->
                        sections.get(effectiveSection)
                                .put(effectiveKey,
                                     c.value());
                        case MultilineStartResult.Error e ->
                        {
                            return e.error();
                        }
                        case MultilineStartResult.Partial p ->
                        multiline = MultilineState.forString(effectiveKey,
                                                             effectiveSection,
                                                             p.partial(),
                                                             true,
                                                             lineNumber);
                    }
                    continue;
                }
                // Check for multiline array start
                if (rawValue.startsWith("[") && !rawValue.endsWith("]")) {
                    if (!isArrayComplete(rawValue)) {
                        multiline = MultilineState.forArray(effectiveKey, effectiveSection, rawValue, lineNumber);
                        continue;
                    }
                }
                // Strip inline comment (but not from quoted strings)
                rawValue = stripInlineComment(rawValue);
                var parseResult = parseValue(rawValue, lineNumber);
                if (parseResult.isFailure()) {
                    return parseResult.map(_ -> null);
                }
                var sect = effectiveSection;
                var k = effectiveKey;
                parseResult.onSuccess(value -> sections.get(sect)
                                                       .put(k, value));
                continue;
            }
            return TomlError.syntaxError(lineNumber, line)
                            .result();
        }
        // Check for unterminated multiline constructs
        if (multiline != null) {
            return multiline.isArray()
                   ? TomlError.unterminatedArray(multiline.startLine())
                              .result()
                   : TomlError.unterminatedMultilineString(multiline.startLine())
                              .result();
        }
        return Result.success(new TomlDocument(Map.copyOf(sections)));
    }

    /// Parsed key-value pair with optional target section for dotted keys.
    private record KeyValue(String key, String targetSection, String value) {}

    /// Try to parse a line as key = value using various key formats.
    private static KeyValue parseKeyValue(String line) {
        // Try quoted key: "key" = value
        var quotedMatcher = QUOTED_KEY_VALUE_PATTERN.matcher(line);
        if (quotedMatcher.matches()) {
            return new KeyValue(quotedMatcher.group(1),
                                null,
                                quotedMatcher.group(2)
                                             .trim());
        }
        // Try literal key: 'key' = value
        var literalMatcher = LITERAL_KEY_VALUE_PATTERN.matcher(line);
        if (literalMatcher.matches()) {
            return new KeyValue(literalMatcher.group(1),
                                null,
                                literalMatcher.group(2)
                                              .trim());
        }
        // Try bare key: key = value
        var bareMatcher = BARE_KEY_VALUE_PATTERN.matcher(line);
        if (bareMatcher.matches()) {
            return new KeyValue(bareMatcher.group(1),
                                null,
                                bareMatcher.group(2)
                                           .trim());
        }
        // Try dotted key: section.key = value
        var dottedMatcher = DOTTED_KEY_VALUE_PATTERN.matcher(line);
        if (dottedMatcher.matches()) {
            String fullKey = dottedMatcher.group(1);
            int lastDot = fullKey.lastIndexOf('.');
            if (lastDot > 0) {
                return new KeyValue(fullKey.substring(lastDot + 1),
                                    fullKey.substring(0, lastDot),
                                    dottedMatcher.group(2)
                                                 .trim());
            }
            return new KeyValue(fullKey,
                                null,
                                dottedMatcher.group(2)
                                             .trim());
        }
        return null;
    }

    /// Check if an array value is complete (balanced brackets).
    private static boolean isArrayComplete(String value) {
        int depth = 0;
        boolean inQuotes = false;
        boolean inSingleQuotes = false;
        for (int i = 0; i < value.length(); i++ ) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length() && (inQuotes || inSingleQuotes)) {
                i++ ;
                // Skip escaped character
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inQuotes = !inQuotes;
            } else if (c == '\'' && !inQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (!inQuotes && !inSingleQuotes) {
                if (c == '[') depth++ ;else if (c == ']') depth-- ;
            }
        }
        return depth == 0;
    }

    /// Handle continuation of multiline array.
    private static Result<Boolean> handleMultilineArrayContinuation(MultilineState state,
                                                                    String rawLine,
                                                                    int lineNumber,
                                                                    Map<String, Map<String, Object>> sections) {
        state.appendLine(rawLine);
        String accumulated = state.content()
                                  .trim();
        if (isArrayComplete(accumulated)) {
            accumulated = stripInlineComment(accumulated);
            var parseResult = parseValue(accumulated, lineNumber);
            if (parseResult.isFailure()) {
                return parseResult.map(_ -> false);
            }
            parseResult.onSuccess(value -> sections.get(state.pendingSection())
                                                   .put(state.pendingKey(),
                                                        value));
            return Result.success(true);
        }
        return Result.success(false);
    }

    /// Result of attempting to parse a multiline string start.
    /// Use pattern matching to extract values - no accessor methods needed.
    private sealed interface MultilineStartResult {
        record Complete(String value) implements MultilineStartResult {}

        record Partial(String partial) implements MultilineStartResult {}

        record Error(Result<TomlDocument> error) implements MultilineStartResult {}
    }

    private static MultilineStartResult handleMultilineStart(String rawValue,
                                                             String delimiter,
                                                             boolean isLiteral,
                                                             int lineNumber) {
        String afterOpen = rawValue.substring(3);
        int closeIndex = afterOpen.indexOf(delimiter);
        if (closeIndex >= 0) {
            // Same-line close: """content""" or '''content'''
            String content = afterOpen.substring(0, closeIndex);
            String processed = isLiteral
                               ? content
                               : processBasicMultilineContent(content);
            return new MultilineStartResult.Complete(processed);
        }
        // Multiline continues on next line
        return new MultilineStartResult.Partial(afterOpen + "\n");
    }

    /// Process multi-line basic string content (with escape sequences and line-ending backslash).
    private static String processBasicMultiline(String raw) {
        // Trim leading newline per TOML spec
        String content = trimLeadingNewline(raw);
        return processBasicMultilineContent(content);
    }

    private static String processBasicMultilineContent(String content) {
        // Handle line-ending backslash and escape sequences
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                if (next == '\n' || next == '\r') {
                    // Line-ending backslash - skip newline and following whitespace
                    i += 2;
                    if (next == '\r' && i < content.length() && content.charAt(i) == '\n') {
                        i++ ;
                    }
                    while (i < content.length() && isWhitespaceOrNewline(content.charAt(i))) {
                        i++ ;
                    }
                    continue;
                }
                // Regular escape sequence - use shared processing
                int consumed = processEscapeSequence(content, i, result);
                if (consumed < 0) {
                    result.append(c);
                    i++ ;
                } else {
                    i += consumed;
                }
            } else {
                result.append(c);
                i++ ;
            }
        }
        return result.toString();
    }

    private static boolean isWhitespaceOrNewline(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /// Process multi-line literal string content (no escape processing).
    private static String processLiteralMultiline(String raw) {
        // Trim leading newline per TOML spec
        return trimLeadingNewline(raw);
    }

    private static String trimLeadingNewline(String s) {
        if (s.startsWith("\r\n")) {
            return s.substring(2);
        }
        if (s.startsWith("\n")) {
            return s.substring(1);
        }
        return s;
    }

    /// Parse TOML content from a file.
    ///
    /// @param path the path to the TOML file
    /// @return Result containing the parsed TomlDocument, or an error
    public static Result<TomlDocument> parseFile(Path path) {
        try{
            return parse(Files.readString(path));
        } catch (IOException e) {
            return TomlError.fileReadFailed(path.toString(),
                                            e.getMessage())
                            .result();
        }
    }

    /// Strip inline comment while properly tracking quote state and bracket depth.
    private static String stripInlineComment(String value) {
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        int bracketDepth = 0;
        for (int i = 0; i < value.length(); i++ ) {
            char c = value.charAt(i);
            // Handle escape sequences in quoted strings
            if ((inDoubleQuotes || inSingleQuotes) && c == '\\' && i + 1 < value.length()) {
                i++ ;
                // Skip next character
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (!inDoubleQuotes && !inSingleQuotes) {
                if (c == '[') {
                    bracketDepth++ ;
                } else if (c == ']') {
                    bracketDepth-- ;
                } else if (c == '#' && bracketDepth == 0) {
                    return value.substring(0, i)
                                .trim();
                }
            }
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
        // Double-quoted string (with escape processing)
        if (value.startsWith("\"")) {
            if (!value.endsWith("\"") || value.length() < 2) {
                return TomlError.unterminatedString(lineNumber)
                                .result();
            }
            return Result.success(unescapeString(value.substring(1, value.length() - 1)));
        }
        // Single-quoted literal string (no escape processing)
        if (value.startsWith("'")) {
            if (!value.endsWith("'") || value.length() < 2) {
                return TomlError.unterminatedString(lineNumber)
                                .result();
            }
            return Result.success(value.substring(1, value.length() - 1));
        }
        // Array
        if (value.startsWith("[")) {
            if (!value.endsWith("]")) {
                return TomlError.unterminatedArray(lineNumber)
                                .result();
            }
            return parseArray(value.substring(1, value.length() - 1),
                              lineNumber);
        }
        // Float (including negative, with decimal point or exponent)
        if (FLOAT_PATTERN.matcher(value)
                         .matches() || FLOAT_EXP_PATTERN.matcher(value)
                                                        .matches()) {
            try{
                return Result.success(Double.parseDouble(value));
            } catch (NumberFormatException _) {
                return TomlError.invalidValue(lineNumber, value, "float")
                                .result();
            }
        }
        // Integer (including negative)
        if (INTEGER_PATTERN.matcher(value)
                           .matches()) {
            try{
                return Result.success(Long.parseLong(value));
            } catch (NumberFormatException _) {
                return TomlError.invalidValue(lineNumber, value, "integer")
                                .result();
            }
        }
        // Unquoted string (identifier-like)
        return Result.success(value);
    }

    private static Result<Object> parseArray(String content, int lineNumber) {
        List<Object> items = new ArrayList<>();
        if (content.trim()
                   .isEmpty()) {
            return Result.success(items);
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int bracketDepth = 0;
        for (int i = 0; i < content.length(); i++ ) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == '[' && !inQuotes) {
                bracketDepth++ ;
                current.append(c);
            } else if (c == ']' && !inQuotes) {
                bracketDepth-- ;
                current.append(c);
            } else if (c == ',' && !inQuotes && bracketDepth == 0) {
                var itemResult = parseValue(current.toString()
                                                   .trim(),
                                            lineNumber);
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
        String lastItem = current.toString()
                                 .trim();
        if (!lastItem.isEmpty()) {
            var itemResult = parseValue(lastItem, lineNumber);
            if (itemResult.isFailure()) {
                return itemResult;
            }
            itemResult.onSuccess(items::add);
        }
        return Result.success(items);
    }

    /// Shared escape sequence processing for basic strings.
    /// Returns the number of characters consumed, or -1 on error.
    private static int processEscapeSequence(String s, int i, StringBuilder result) {
        if (i + 1 >= s.length()) {
            return - 1;
        }
        char next = s.charAt(i + 1);
        return switch (next) {
            case '\\' -> {
                result.append('\\');
                yield 2;
            }
            case '"' -> {
                result.append('"');
                yield 2;
            }
            case 'n' -> {
                result.append('\n');
                yield 2;
            }
            case 't' -> {
                result.append('\t');
                yield 2;
            }
            case 'r' -> {
                result.append('\r');
                yield 2;
            }
            case 'b' -> {
                result.append('\b');
                yield 2;
            }
            case 'f' -> {
                result.append('\f');
                yield 2;
            }
            case 'u' -> processUnicodeEscape(s, i, 4, result);
            case 'U' -> processUnicodeEscape(s, i, 8, result);
            default -> - 1;
        };
    }

    /// Process unicode escape sequences (4 or 8 hex digits).
    private static int processUnicodeEscape(String s, int i, int digits, StringBuilder result) {
        // Need: \ + u/U + digits
        if (i + 2 + digits > s.length()) {
            return - 1;
        }
        String hex = s.substring(i + 2, i + 2 + digits);
        try{
            int codePoint = Integer.parseInt(hex, 16);
            result.appendCodePoint(codePoint);
            return 2 + digits;
        } catch (NumberFormatException _) {
            return - 1;
        }
    }

    private static String unescapeString(String s) {
        var result = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                int consumed = processEscapeSequence(s, i, result);
                if (consumed < 0) {
                    // Invalid escape - append backslash and continue
                    result.append(c);
                    i++ ;
                } else {
                    i += consumed;
                }
            } else {
                result.append(c);
                i++ ;
            }
        }
        return result.toString();
    }
}
