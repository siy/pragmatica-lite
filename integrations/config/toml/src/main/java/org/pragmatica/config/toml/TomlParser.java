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
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Option.some;

/// Zero-dependency TOML parser supporting a practical subset of TOML 1.0.
///
/// Supported features:
///
///   - Sections: `[section]` and `[section.subsection]`
///   - Array of tables: `[[array]]` with nested sub-tables
///   - Properties: `key = value`
///   - Quoted strings: `"hello world"` with escape sequences
///   - Literal strings: `'hello world'` without escape processing
///   - Multi-line basic strings: `"""..."""` with escape sequences
///   - Multi-line literal strings: `'''...'''` without escape processing
///   - Booleans: `true` / `false`
///   - Integers: `123`, `-456`, `1_000_000`, `0xDEAD`, `0o755`, `0b1101`
///   - Floating point numbers: `3.14`, `-0.5e10`, `inf`, `-inf`, `nan`
///   - Arrays: `["a", "b", "c"]`
///   - Comments: `# comment`
///
/// Not yet supported:
///
///   - Inline tables: `{key = value}`
///   - Dates and times
///
/// Example usage:
/// ```java
/// TomlParser.parse(content)
///     .onSuccess(doc -> {
///         doc.getString("database", "host").onPresent(System.out::println);
///         doc.getInt("server", "port").orElse(8080);
///     })
///     .onFailure(error -> System.err.println(error.message()));
/// ```
public final class TomlParser {
    // Section patterns - support quoted keys in section headers
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_.\\-]+|\"[^\"]*\"|'[^']*')]$");
    private static final Pattern ARRAY_TABLE_PATTERN = Pattern.compile("^\\[\\[([a-zA-Z0-9_.\\-]+|\"[^\"]*\"|'[^']*')]]$");

    // Key patterns - support empty quoted keys
    private static final Pattern BARE_KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+)\\s*=\\s*(.+)$");
    private static final Pattern QUOTED_KEY_VALUE_PATTERN = Pattern.compile("^\"([^\"]*)\"\\s*=\\s*(.+)$");
    private static final Pattern LITERAL_KEY_VALUE_PATTERN = Pattern.compile("^'([^']*)'\\s*=\\s*(.+)$");
    private static final Pattern DOTTED_KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_.\\-]+)\\s*=\\s*(.+)$");

    // Number patterns with underscore support
    private static final Pattern FLOAT_PATTERN = Pattern.compile("-?[0-9][0-9_]*\\.[0-9_]+([eE][+-]?[0-9_]+)?");
    private static final Pattern FLOAT_EXP_PATTERN = Pattern.compile("-?[0-9][0-9_]*[eE][+-]?[0-9_]+");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?(?:0|[1-9][0-9_]*)");
    private static final Pattern HEX_PATTERN = Pattern.compile("0x[0-9a-fA-F_]+");
    private static final Pattern OCTAL_PATTERN = Pattern.compile("0o[0-7_]+");
    private static final Pattern BINARY_PATTERN = Pattern.compile("0b[01_]+");

    // Special float values
    private static final Pattern SPECIAL_FLOAT_PATTERN = Pattern.compile("[+-]?(inf|nan)");

    // Inline table pattern for detection
    private static final Pattern INLINE_TABLE_PATTERN = Pattern.compile("^\\{.*}$");

    // Date patterns for detection
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}.*");

    private TomlParser() {}

    /// Encapsulates mutable state for parsing multiline constructs (strings and arrays).
    private static final class MultilineState {
        private final String pendingKey;
        private final String pendingSection;
        private final StringBuilder builder;
        private final boolean isLiteral;
        private final boolean isArray;
        private final int startLine;

        private MultilineState(String key, String section, String initial, boolean literal, boolean array, int line) {
            this.pendingKey = key;
            this.pendingSection = section;
            this.builder = new StringBuilder(initial);
            this.isLiteral = literal;
            this.isArray = array;
            this.startLine = line;
        }

        static MultilineState forString(String key, String section, String initial, boolean literal, int line) {
            return new MultilineState(key, section, initial, literal, false, line);
        }

        static MultilineState forArray(String key, String section, String initial, int line) {
            return new MultilineState(key, section, initial, false, true, line);
        }

        String pendingKey() {
            return pendingKey;
        }

        String pendingSection() {
            return pendingSection;
        }

        boolean isLiteral() {
            return isLiteral;
        }

        boolean isArray() {
            return isArray;
        }

        int startLine() {
            return startLine;
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

    /// Context for tracking the current array table being populated.
    private record ArrayTableContext(String base, Map<String, Object> element) {}

    /// Parse TOML content from a string.
    ///
    /// @param content the TOML content to parse
    /// @return Result containing the parsed TomlDocument, or an error
    public static Result<TomlDocument> parse(String content) {
        return option(content).filter(c -> !c.isBlank())
                     .fold(() -> Result.success(TomlDocument.EMPTY),
                           TomlParser::parseNonEmpty);
    }

    private static Result<TomlDocument> parseNonEmpty(String content) {
        Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> arrayTables = new LinkedHashMap<>();
        Set<String> definedSections = new HashSet<>();
        sections.put("", new LinkedHashMap<>());
        String currentSection = "";
        Option<ArrayTableContext> arrayTableContext = none();
        int lineNumber = 0;
        Option<MultilineState> multiline = none();
        String[] lines = content.split("\n", - 1);
        for (String rawLine : lines) {
            lineNumber++;
            final int currentLineNumber = lineNumber;
            // Handle multiline array continuation
            if (multiline.filter(MultilineState::isArray)
                         .isPresent()) {
                var state = multiline.unwrap();
                var result = handleMultilineArrayContinuation(state,
                                                              rawLine,
                                                              currentLineNumber,
                                                              sections,
                                                              arrayTables,
                                                              arrayTableContext);
                if (result.isFailure()) {
                    return result.map(_ -> null);
                }
                if (result.fold(_ -> false, completed -> completed)) {
                    multiline = none();
                }
                continue;
            }
            // Handle multiline string continuation
            if (multiline.isPresent()) {
                var state = multiline.unwrap();
                String delimiter = state.isLiteral()
                                   ? "'''"
                                   : "\"\"\"";
                int endIndex = findClosingDelimiter(rawLine, delimiter);
                if (endIndex >= 0) {
                    state.append(rawLine.substring(0, endIndex));
                    var processResult = state.isLiteral()
                                        ? Result.success(processLiteralMultiline(state.content()))
                                        : processBasicMultiline(state.content(), state.startLine());
                    if (processResult.isFailure()) {
                        return processResult.map(_ -> null);
                    }
                    String processed = processResult.fold(_ -> "", s -> s);
                    putValue(sections,
                             arrayTables,
                             arrayTableContext,
                             state.pendingSection(),
                             state.pendingKey(),
                             processed);
                    multiline = none();
                } else {
                    state.appendLine(rawLine);
                }
                continue;
            }
            String line = rawLine.trim();
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // Check for array of tables header [[name]]
            var arrayTableMatcher = ARRAY_TABLE_PATTERN.matcher(line);
            if (arrayTableMatcher.matches()) {
                String tableName = normalizeTableName(arrayTableMatcher.group(1));
                // Check for type mismatch - section already defined as regular table
                if (sections.containsKey(tableName)) {
                    return TomlError.tableTypeMismatch(currentLineNumber, tableName, "regular table")
                                    .result();
                }
                // Create new table in the array
                var newTable = new LinkedHashMap<String, Object>();
                arrayTables.computeIfAbsent(tableName,
                                            _ -> new ArrayList<>())
                           .add(newTable);
                arrayTableContext = some(new ArrayTableContext(tableName, newTable));
                currentSection = tableName;
                continue;
            }
            // Check for section header [name]
            var sectionMatcher = SECTION_PATTERN.matcher(line);
            if (sectionMatcher.matches()) {
                String sectionName = normalizeTableName(sectionMatcher.group(1));
                // Check if this is a sub-table within an array element
                if (arrayTableContext.filter(ctx -> sectionName.startsWith(ctx.base() + "."))
                                     .isPresent()) {
                    currentSection = sectionName;
                    continue;
                }
                // Check for type mismatch - section already defined as array table
                if (arrayTables.containsKey(sectionName)) {
                    return TomlError.tableTypeMismatch(currentLineNumber, sectionName, "array of tables")
                                    .result();
                }
                // Check for duplicate section definition
                if (definedSections.contains(sectionName)) {
                    return TomlError.duplicateSection(currentLineNumber, sectionName)
                                    .result();
                }
                definedSections.add(sectionName);
                // Regular section - exit array table context
                arrayTableContext = none();
                currentSection = sectionName;
                sections.putIfAbsent(currentSection, new LinkedHashMap<>());
                continue;
            }
            // Try to parse key = value with different key formats
            var keyValueOpt = parseKeyValue(line, currentLineNumber);
            if (keyValueOpt.isPresent()) {
                var keyValueResult = keyValueOpt.unwrap();
                String key = keyValueResult.key();
                Option<String> targetSection = keyValueResult.targetSection();
                String rawValue = keyValueResult.value();
                // Handle dotted keys - create implicit sections
                final String effectiveSection;
                String effectiveKey = key;
                final Option<ArrayTableContext> capturedContext = arrayTableContext;
                if (targetSection.isPresent()) {
                    var target = targetSection.unwrap();
                    effectiveSection = currentSection.isEmpty()
                                       ? target
                                       : currentSection + "." + target;
                    if (arrayTableContext.isEmpty()) {
                        // Check for dotted key conflict
                        var conflictResult = checkDottedKeyConflict(sections, currentSection, target, currentLineNumber);
                        if (conflictResult.isFailure()) {
                            return conflictResult.map(_ -> null);
                        }
                        sections.putIfAbsent(effectiveSection, new LinkedHashMap<>());
                    }
                } else {
                    effectiveSection = currentSection;
                }
                // Check for duplicate key
                if (isDuplicateKey(sections, arrayTables, arrayTableContext, effectiveSection, effectiveKey)) {
                    return TomlError.duplicateKey(currentLineNumber, effectiveKey)
                                    .result();
                }
                // Check for multiline string start
                if (rawValue.startsWith("\"\"\"")) {
                    var result = handleMultilineStart(rawValue, "\"\"\"", false, currentLineNumber);
                    switch (result) {
                        case MultilineStartResult.Complete c -> {
                            var processResult = processBasicMultilineContent(c.rawValue(), currentLineNumber);
                            if (processResult.isFailure()) {
                                return processResult.map(_ -> null);
                            }
                            putValue(sections,
                                     arrayTables,
                                     capturedContext,
                                     effectiveSection,
                                     effectiveKey,
                                     processResult.fold(_ -> "", s -> s));
                        }
                        case MultilineStartResult.Error e -> {
                            return e.error();
                        }
                        case MultilineStartResult.Partial p ->
                        multiline = some(MultilineState.forString(effectiveKey,
                                                                  effectiveSection,
                                                                  p.partial(),
                                                                  false,
                                                                  currentLineNumber));
                    }
                    continue;
                }
                if (rawValue.startsWith("'''")) {
                    var result = handleMultilineStart(rawValue, "'''", true, currentLineNumber);
                    switch (result) {
                        case MultilineStartResult.Complete c ->
                        putValue(sections, arrayTables, capturedContext, effectiveSection, effectiveKey, c.rawValue());
                        case MultilineStartResult.Error e -> {
                            return e.error();
                        }
                        case MultilineStartResult.Partial p ->
                        multiline = some(MultilineState.forString(effectiveKey,
                                                                  effectiveSection,
                                                                  p.partial(),
                                                                  true,
                                                                  currentLineNumber));
                    }
                    continue;
                }
                // Check for multiline array start
                if (rawValue.startsWith("[") && !rawValue.endsWith("]")) {
                    if (!isArrayComplete(rawValue)) {
                        multiline = some(MultilineState.forArray(effectiveKey,
                                                                 effectiveSection,
                                                                 rawValue,
                                                                 currentLineNumber));
                        continue;
                    }
                }
                // Strip inline comment (but not from quoted strings)
                rawValue = stripInlineComment(rawValue);
                var parseResult = parseValue(rawValue, currentLineNumber);
                if (parseResult.isFailure()) {
                    return parseResult.map(_ -> null);
                }
                var sect = effectiveSection;
                var k = effectiveKey;
                parseResult.onSuccess(value -> putValue(sections, arrayTables, capturedContext, sect, k, value));
                continue;
            }
            return TomlError.syntaxError(currentLineNumber, line)
                            .result();
        }
        // Check for unterminated multiline constructs
        if (multiline.isPresent()) {
            var state = multiline.unwrap();
            return state.isArray()
                   ? TomlError.unterminatedArray(state.startLine())
                              .result()
                   : TomlError.unterminatedMultilineString(state.startLine())
                              .result();
        }
        return Result.success(new TomlDocument(sections, copyArrayTables(arrayTables)));
    }

    /// Normalize a table name - handle quoted parts.
    private static String normalizeTableName(String name) {
        // Remove quotes from quoted key
        if (name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1);
        }
        if (name.startsWith("'") && name.endsWith("'")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    /// Check for dotted key conflicts where a value blocks a table path.
    private static Result<Unit> checkDottedKeyConflict(Map<String, Map<String, Object>> sections,
                                                       String currentSection,
                                                       String targetSection,
                                                       int lineNumber) {
        String[] parts = targetSection.split("\\.");
        StringBuilder path = new StringBuilder(currentSection);
        for (int i = 0; i < parts.length - 1; i++) {
            if (!path.isEmpty()) {
                path.append(".");
            }
            path.append(parts[i]);
            String pathStr = path.toString();
            String sectionKey = pathStr.isEmpty()
                                ? ""
                                : pathStr.substring(0,
                                                    pathStr.lastIndexOf('.') < 0
                                                    ? pathStr.length()
                                                    : pathStr.lastIndexOf('.'));
            String partKey = parts[i];
            boolean isConflict = option(sections.get(sectionKey)).filter(section -> section.containsKey(partKey))
                                       .filter(section -> !(section.get(partKey) instanceof Map))
                                       .isPresent();
            if (isConflict) {
                return TomlError.dottedKeyConflict(lineNumber, targetSection, pathStr)
                                .result();
            }
        }
        return Result.unitResult();
    }

    /// Find closing delimiter handling quotes before it.
    private static int findClosingDelimiter(String line, String delimiter) {
        int idx = line.indexOf(delimiter);
        if (idx < 0) {
            return - 1;
        }
        // For """, check if there are extra quotes
        if (delimiter.equals("\"\"\"")) {
            // Count consecutive quotes
            int end = idx + 3;
            while (end < line.length() && line.charAt(end) == '"') {
                end++;
            }
            // Up to 2 extra quotes allowed before closing
            int extraQuotes = end - idx - 3;
            if (extraQuotes <= 2) {
                return idx + extraQuotes;
            }
        }
        return idx;
    }

    /// Put a value into the appropriate location (regular section or array table element).
    private static void putValue(Map<String, Map<String, Object>> sections,
                                 Map<String, List<Map<String, Object>>> arrayTables,
                                 Option<ArrayTableContext> arrayTableContext,
                                 String section,
                                 String key,
                                 Object value) {
        arrayTableContext.filter(ctx -> section.startsWith(ctx.base()))
                         .fold(() -> {
                                   sections.computeIfAbsent(section,
                                                            _ -> new LinkedHashMap<>())
                                           .put(key, value);
                                   return Unit.unit();
                               },
                               ctx -> {
                                   if (section.equals(ctx.base())) {
                                       ctx.element()
                                          .put(key, value);
                                   } else {
                                       String subPath = section.substring(ctx.base()
                                                                             .length() + 1);
                                       getOrCreateNestedMap(ctx.element(),
                                                            subPath).put(key, value);
                                   }
                                   return Unit.unit();
                               });
    }

    /// Get or create a nested map for sub-table paths like "details" or "nested.deep".
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getOrCreateNestedMap(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (String part : parts) {
            current = (Map<String, Object>) current.computeIfAbsent(part, _ -> new LinkedHashMap<String, Object>());
        }
        return current;
    }

    /// Check if a key already exists in the appropriate location.
    private static boolean isDuplicateKey(Map<String, Map<String, Object>> sections,
                                          Map<String, List<Map<String, Object>>> arrayTables,
                                          Option<ArrayTableContext> arrayTableContext,
                                          String section,
                                          String key) {
        return arrayTableContext.filter(ctx -> section.startsWith(ctx.base()))
                                .fold(() -> option(sections.get(section)).filter(sectionMap -> sectionMap.containsKey(key))
                                                  .isPresent(),
                                      ctx -> {
                                          if (section.equals(ctx.base())) {
                                              return ctx.element()
                                                        .containsKey(key);
                                          } else {
                                              String subPath = section.substring(ctx.base()
                                                                                    .length() + 1);
                                              return getNestedMap(ctx.element(),
                                                                  subPath).filter(nested -> nested.containsKey(key))
                                                                 .isPresent();
                                          }
                                      });
    }

    /// Get a nested map without creating it.
    @SuppressWarnings("unchecked")
    private static Option<Map<String, Object>> getNestedMap(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (String part : parts) {
            Object next = current.get(part);
            if (next instanceof Map<?, ?> map) {
                current = (Map<String, Object>) map;
            } else {
                return none();
            }
        }
        return some(current);
    }

    /// Create an immutable copy of array tables.
    private static Map<String, List<Map<String, Object>>> copyArrayTables(Map<String, List<Map<String, Object>>> arrayTables) {
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        arrayTables.forEach((name, tables) -> result.put(name, List.copyOf(tables)));
        return Map.copyOf(result);
    }

    /// Parsed key-value pair with optional target section for dotted keys.
    private record KeyValue(String key, Option<String> targetSection, String value) {}

    /// Try to parse a line as key = value using various key formats.
    private static Option<KeyValue> parseKeyValue(String line, int lineNumber) {
        // Try quoted key: "key" = value
        var quotedMatcher = QUOTED_KEY_VALUE_PATTERN.matcher(line);
        if (quotedMatcher.matches()) {
            String quotedKey = quotedMatcher.group(1);
            // Process escape sequences in quoted keys
            var unescaped = unescapeBasicString(quotedKey, lineNumber);
            String key = unescaped.fold(_ -> quotedKey, s -> s);
            return some(new KeyValue(key,
                                     none(),
                                     quotedMatcher.group(2)
                                                  .trim()));
        }
        // Try literal key: 'key' = value
        var literalMatcher = LITERAL_KEY_VALUE_PATTERN.matcher(line);
        if (literalMatcher.matches()) {
            return some(new KeyValue(literalMatcher.group(1),
                                     none(),
                                     literalMatcher.group(2)
                                                   .trim()));
        }
        // Try bare key: key = value
        var bareMatcher = BARE_KEY_VALUE_PATTERN.matcher(line);
        if (bareMatcher.matches()) {
            return some(new KeyValue(bareMatcher.group(1),
                                     none(),
                                     bareMatcher.group(2)
                                                .trim()));
        }
        // Try dotted key: section.key = value
        var dottedMatcher = DOTTED_KEY_VALUE_PATTERN.matcher(line);
        if (dottedMatcher.matches()) {
            String fullKey = dottedMatcher.group(1);
            int lastDot = fullKey.lastIndexOf('.');
            if (lastDot > 0) {
                return some(new KeyValue(fullKey.substring(lastDot + 1),
                                         some(fullKey.substring(0, lastDot)),
                                         dottedMatcher.group(2)
                                                      .trim()));
            }
            return some(new KeyValue(fullKey,
                                     none(),
                                     dottedMatcher.group(2)
                                                  .trim()));
        }
        return none();
    }

    /// Check if an array value is complete (balanced brackets).
    private static boolean isArrayComplete(String value) {
        int depth = 0;
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Handle escape sequences only in double-quoted strings
            if (c == '\\' && i + 1 < value.length() && inDoubleQuotes) {
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (!inDoubleQuotes && !inSingleQuotes) {
                if (c == '[') depth++;else if (c == ']') depth--;
            }
        }
        return depth == 0;
    }

    /// Handle continuation of multiline array.
    private static Result<Boolean> handleMultilineArrayContinuation(MultilineState state,
                                                                    String rawLine,
                                                                    int lineNumber,
                                                                    Map<String, Map<String, Object>> sections,
                                                                    Map<String, List<Map<String, Object>>> arrayTables,
                                                                    Option<ArrayTableContext> arrayTableContext) {
        // Strip comments from array lines
        String processedLine = stripArrayLineComment(rawLine);
        state.appendLine(processedLine);
        String accumulated = state.content()
                                  .trim();
        if (isArrayComplete(accumulated)) {
            accumulated = stripInlineComment(accumulated);
            var parseResult = parseValue(accumulated, lineNumber);
            if (parseResult.isFailure()) {
                return parseResult.map(_ -> false);
            }
            parseResult.onSuccess(value -> putValue(sections,
                                                    arrayTables,
                                                    arrayTableContext,
                                                    state.pendingSection(),
                                                    state.pendingKey(),
                                                    value));
            return Result.success(true);
        }
        return Result.success(false);
    }

    /// Strip comment from array line (outside of quotes).
    private static String stripArrayLineComment(String line) {
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length() && inDoubleQuotes) {
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == '#' && !inDoubleQuotes && !inSingleQuotes) {
                return line.substring(0, i)
                           .trim();
            }
        }
        return line;
    }

    /// Result of attempting to parse a multiline string start.
    private sealed interface MultilineStartResult {
        record Complete(String rawValue) implements MultilineStartResult {}

        record Partial(String partial) implements MultilineStartResult {}

        record Error(Result<TomlDocument> error) implements MultilineStartResult {}
    }

    private static MultilineStartResult handleMultilineStart(String rawValue,
                                                             String delimiter,
                                                             boolean isLiteral,
                                                             int lineNumber) {
        String afterOpen = rawValue.substring(3);
        int closeIndex = findClosingDelimiter(afterOpen, delimiter);
        if (closeIndex >= 0) {
            // Same-line close: """content""" or '''content'''
            String content = afterOpen.substring(0, closeIndex);
            return new MultilineStartResult.Complete(content);
        }
        // Multiline continues on next line
        return new MultilineStartResult.Partial(afterOpen + "\n");
    }

    /// Process multi-line basic string content (with escape sequences and line-ending backslash).
    private static Result<String> processBasicMultiline(String raw, int startLine) {
        String content = trimLeadingNewline(raw);
        return processBasicMultilineContent(content, startLine);
    }

    private static Result<String> processBasicMultilineContent(String content, int startLine) {
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
                        i++;
                    }
                    while (i < content.length() && isWhitespaceOrNewline(content.charAt(i))) {
                        i++;
                    }
                    continue;
                }
                // Regular escape sequence
                var escapeResult = processEscapeSequence(content, i, startLine);
                if (escapeResult.isFailure()) {
                    return escapeResult.map(_ -> "");
                }
                var pair = escapeResult.fold(_ -> new EscapeResult("", 1), p -> p);
                result.append(pair.value());
                i += pair.consumed();
            } else {
                result.append(c);
                i++;
            }
        }
        return Result.success(result.toString());
    }

    private static boolean isWhitespaceOrNewline(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /// Process multi-line literal string content (no escape processing).
    private static String processLiteralMultiline(String raw) {
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
        return Result.lift(e -> TomlError.fileReadFailed(path.toString(),
                                                         e.getMessage()),
                           () -> Files.readString(path))
                     .flatMap(TomlParser::parse);
    }

    /// Strip inline comment while properly tracking quote state and bracket depth.
    private static String stripInlineComment(String value) {
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        int bracketDepth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Handle escape sequences only in double-quoted strings
            if (inDoubleQuotes && c == '\\' && i + 1 < value.length()) {
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (!inDoubleQuotes && !inSingleQuotes) {
                if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
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
        // Special float values (inf, nan)
        if (SPECIAL_FLOAT_PATTERN.matcher(value)
                                 .matches()) {
            return Result.success(parseSpecialFloat(value));
        }
        // Double-quoted string (with escape processing)
        if (value.startsWith("\"")) {
            if (!value.endsWith("\"") || value.length() < 2) {
                return TomlError.unterminatedString(lineNumber)
                                .result();
            }
            return unescapeString(value.substring(1, value.length() - 1),
                                  lineNumber);
        }
        // Single-quoted literal string (no escape processing)
        if (value.startsWith("'")) {
            if (!value.endsWith("'") || value.length() < 2) {
                return TomlError.unterminatedString(lineNumber)
                                .result();
            }
            return Result.success(value.substring(1, value.length() - 1));
        }
        // Inline table detection
        if (INLINE_TABLE_PATTERN.matcher(value)
                                .matches()) {
            return TomlError.unsupportedFeature(lineNumber, "inline tables")
                            .result();
        }
        // Date/time detection
        if (DATE_PATTERN.matcher(value)
                        .matches()) {
            return TomlError.unsupportedFeature(lineNumber, "dates and times")
                            .result();
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
        // Hex integer
        if (HEX_PATTERN.matcher(value)
                       .matches()) {
            return parseHexInteger(value, lineNumber);
        }
        // Octal integer
        if (OCTAL_PATTERN.matcher(value)
                         .matches()) {
            return parseOctalInteger(value, lineNumber);
        }
        // Binary integer
        if (BINARY_PATTERN.matcher(value)
                          .matches()) {
            return parseBinaryInteger(value, lineNumber);
        }
        // Float (including negative, with decimal point or exponent)
        if (FLOAT_PATTERN.matcher(value)
                         .matches() || FLOAT_EXP_PATTERN.matcher(value)
                                                        .matches()) {
            return parseFloat(value, lineNumber);
        }
        // Integer (including negative) - check for leading zeros
        if (INTEGER_PATTERN.matcher(value)
                           .matches()) {
            return parseInteger(value, lineNumber);
        }
        // Check for invalid leading zeros
        if (value.matches("-?0\\d+")) {
            return TomlError.invalidValue(lineNumber,
                                          value,
                                          "integer (leading zeros not allowed)")
                            .result();
        }
        // Unquoted string (identifier-like)
        return Result.success(value);
    }

    private static Double parseSpecialFloat(String value) {
        return switch (value) {
            case "inf", "+inf" -> Double.POSITIVE_INFINITY;
            case "-inf" -> Double.NEGATIVE_INFINITY;
            case "nan", "+nan", "-nan" -> Double.NaN;
            default -> Double.NaN;
        };
    }

    private static Result<Object> parseFloat(String value, int lineNumber) {
        try{
            String cleaned = value.replace("_", "");
            return Result.success(Double.parseDouble(cleaned));
        } catch (NumberFormatException _) {
            return TomlError.invalidValue(lineNumber, value, "float")
                            .result();
        }
    }

    private static Result<Object> parseInteger(String value, int lineNumber) {
        try{
            String cleaned = value.replace("_", "");
            return Result.success(Long.parseLong(cleaned));
        } catch (NumberFormatException _) {
            return TomlError.invalidValue(lineNumber, value, "integer")
                            .result();
        }
    }

    private static Result<Object> parseHexInteger(String value, int lineNumber) {
        try{
            String cleaned = value.substring(2)
                                  .replace("_", "");
            return Result.success(Long.parseLong(cleaned, 16));
        } catch (NumberFormatException _) {
            return TomlError.invalidValue(lineNumber, value, "hexadecimal integer")
                            .result();
        }
    }

    private static Result<Object> parseOctalInteger(String value, int lineNumber) {
        try{
            String cleaned = value.substring(2)
                                  .replace("_", "");
            return Result.success(Long.parseLong(cleaned, 8));
        } catch (NumberFormatException _) {
            return TomlError.invalidValue(lineNumber, value, "octal integer")
                            .result();
        }
    }

    private static Result<Object> parseBinaryInteger(String value, int lineNumber) {
        try{
            String cleaned = value.substring(2)
                                  .replace("_", "");
            return Result.success(Long.parseLong(cleaned, 2));
        } catch (NumberFormatException _) {
            return TomlError.invalidValue(lineNumber, value, "binary integer")
                            .result();
        }
    }

    private static Result<Object> parseArray(String content, int lineNumber) {
        List<Object> items = new ArrayList<>();
        if (content.trim()
                   .isEmpty()) {
            return Result.success(items);
        }
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        int bracketDepth = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            // Handle escapes only in double-quoted strings
            if (c == '\\' && i + 1 < content.length() && inDoubleQuotes) {
                current.append(c);
                current.append(content.charAt(i + 1));
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                current.append(c);
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                current.append(c);
            } else if (c == '[' && !inDoubleQuotes && !inSingleQuotes) {
                bracketDepth++;
                current.append(c);
            } else if (c == ']' && !inDoubleQuotes && !inSingleQuotes) {
                bracketDepth--;
                current.append(c);
            } else if (c == ',' && !inDoubleQuotes && !inSingleQuotes && bracketDepth == 0) {
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

    /// Result of processing an escape sequence.
    private record EscapeResult(String value, int consumed) {}

    /// Process escape sequence and return the result with characters consumed.
    private static Result<EscapeResult> processEscapeSequence(String s, int i, int lineNumber) {
        if (i + 1 >= s.length()) {
            return TomlError.invalidEscapeSequence(lineNumber, "\\")
                            .result();
        }
        char next = s.charAt(i + 1);
        return switch (next) {
            case '\\' -> Result.success(new EscapeResult("\\", 2));
            case '"' -> Result.success(new EscapeResult("\"", 2));
            case 'n' -> Result.success(new EscapeResult("\n", 2));
            case 't' -> Result.success(new EscapeResult("\t", 2));
            case 'r' -> Result.success(new EscapeResult("\r", 2));
            case 'b' -> Result.success(new EscapeResult("\b", 2));
            case 'f' -> Result.success(new EscapeResult("\f", 2));
            case 'u' -> processUnicodeEscape(s, i, 4, lineNumber);
            case 'U' -> processUnicodeEscape(s, i, 8, lineNumber);
            default -> TomlError.invalidEscapeSequence(lineNumber,
                                                       String.valueOf(next))
                                .result();
        };
    }

    /// Process unicode escape sequences (4 or 8 hex digits).
    private static Result<EscapeResult> processUnicodeEscape(String s, int i, int digits, int lineNumber) {
        if (i + 2 + digits > s.length()) {
            return TomlError.invalidEscapeSequence(lineNumber,
                                                   s.substring(i,
                                                               Math.min(i + 2 + digits,
                                                                        s.length())))
                            .result();
        }
        String hex = s.substring(i + 2, i + 2 + digits);
        try{
            int codePoint = Integer.parseInt(hex, 16);
            // Check for invalid surrogates (D800-DFFF)
            if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
                return TomlError.invalidSurrogate(lineNumber, hex)
                                .result();
            }
            return Result.success(new EscapeResult(Character.toString(codePoint), 2 + digits));
        } catch (NumberFormatException _) {
            return TomlError.invalidEscapeSequence(lineNumber,
                                                   s.substring(i, i + 2 + digits))
                            .result();
        }
    }

    /// Unescape a basic string and return the result as String (for keys).
    private static Result<String> unescapeBasicString(String s, int lineNumber) {
        var result = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                var escapeResult = processEscapeSequence(s, i, lineNumber);
                if (escapeResult.isFailure()) {
                    return escapeResult.map(_ -> "");
                }
                var pair = escapeResult.fold(_ -> new EscapeResult("", 1), p -> p);
                result.append(pair.value());
                i += pair.consumed();
            } else {
                result.append(c);
                i++;
            }
        }
        return Result.success(result.toString());
    }

    /// Unescape a string value and return as Object (for values).
    private static Result<Object> unescapeString(String s, int lineNumber) {
        return unescapeBasicString(s, lineNumber).map(str -> str);
    }
}
