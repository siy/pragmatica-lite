package org.pragmatica.config.format.yaml;

import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Yaml file reader. Based on <a href="https://github.com/tvd12/properties-file">properties-file</a> project with significant changes.
 */
public final class YamlFileReader {
    private static final char TAB_CHAR = '\t';
    private static final char SPACE_CHAR = ' ';
    private static final char DASH_CHAR = '-';
    private static final char HASH_CHAR = '#';
    private static final char DOUBLE_QUOTE_CHAR = '"';
    private static final char SINGLE_QUOTE_CHAR = '\'';

    private static final String COLON_CHAR = ":";
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+");

    private static class ReaderState {
        int lineIndex = 0;
        int lastSpaceCount = 0;
        String line;
        String lastParentKey = null;
        String currentKey = null;
        HashMap<String, String> map = new HashMap<>();
        TreeMap<Integer, String> nodes = new TreeMap<>();

        private String extractKey(String rawKey) {
            var key = rawKey.charAt(0) == DASH_CHAR
                      ? rawKey.substring(1).trim()
                      : rawKey;

            return !KEY_PATTERN.matcher(key).matches() ? null : key;
        }

        private String extractValue(String rawValue) {
            var value = stripTrailingComment(rawValue.trim());

            if (isQuotedString(value, DOUBLE_QUOTE_CHAR) || isQuotedString(value, SINGLE_QUOTE_CHAR)) {
                return value.length() > 2
                       ? value.substring(1, value.length() - 1)
                       : "";
            }

            if (value.startsWith(">-")) {
                return value.length() > 2
                       ? value.substring(2).trim()
                       : "";
            }

            if (value.startsWith("|") || value.startsWith(">")) {
                return value.length() > 1
                       ? value.substring(1).trim()
                       : "";
            }

            return value.trim();
        }

        private static boolean isQuotedString(String string, char quoteType) {
            return string.charAt(0) == quoteType
                   && string.charAt(string.length() - 1) == quoteType;
        }

        private static String stripTrailingComment(String line) {
            int commentIndex = line.indexOf(HASH_CHAR);

            return commentIndex >= 0 ? line.substring(0, commentIndex).trim() : line;
        }

        private static int countLeadingIndent(String line) {
            return line.chars().takeWhile(c -> c == SPACE_CHAR || c == TAB_CHAR)
                        .reduce(0, (acc, c) -> acc + (c == SPACE_CHAR ? 1 : 4));
        }

        Result<StringMap> read(Iterator<String> lines) {
            while (lines.hasNext()) {
                line = lines.next();
                ++lineIndex;

                var lineTrim = line.trim();

                if (lineTrim.isEmpty() || lineTrim.charAt(0) == HASH_CHAR) {
                    continue;
                }

                if (!lineTrim.contains(COLON_CHAR)) {
                    if (currentKey == null) {
                        return YamlParseError.invalidSyntax("", lineIndex, line).result();
                    } else {
                        if (lineTrim.charAt(0) == DASH_CHAR) {
                            if (lineTrim.length() > 1) {
                                var value = lineTrim.substring(1).trim();

                                map.compute(currentKey, (_, v) -> v == null ? value : (v + "," + value));
                            }
                        } else {
                            map.compute(currentKey, (_, v) -> v == null ? lineTrim : (v + " " + lineTrim));
                        }
                        continue;
                    }
                }

                var keyValue = lineTrim.split(COLON_CHAR, 2);
                var keyTrim = keyValue[0].trim();

                if (keyTrim.isEmpty()) {
                    return YamlParseError.invalidSyntax("missing key", lineIndex, line).result();
                }

                var key = extractKey(keyTrim);

                if (key == null) {
                    int spaceCount = countLeadingIndent(line);

                    if (currentKey == null || spaceCount <= lastSpaceCount) {
                        return YamlParseError.invalidSyntax("invalid key " + keyTrim, lineIndex, line).result();
                    }

                    map.compute(currentKey, (_, v) -> v == null
                                                      ? lineTrim
                                                      : v + " " + lineTrim);
                    continue;
                }

                int spaceCount = countLeadingIndent(line);
                var parentEntry = nodes.lowerEntry(spaceCount);
                var parentNode = parentEntry != null ? parentEntry.getValue() : null;

                if (keyValue[1].isBlank()) {
                    lastParentKey = parentNode != null
                                    ? parentNode + "." + keyTrim
                                    : key;
                    nodes.put(spaceCount, lastParentKey);
                    currentKey = lastParentKey;
                } else {
                    if (parentNode == null) {
                        lastParentKey = null;
                    }

                    var fullKey = lastParentKey != null ? lastParentKey + "." + key : key;

                    map.put(fullKey, extractValue(keyValue[1]));
                    currentKey = fullKey;
                }
                lastSpaceCount = spaceCount;
            }
            return Result.success(() -> map);
        }
    }

    public static Result<StringMap> readFile(String source) {
        return Result.lift(Causes::fromThrowable, () -> Files.readAllLines(Path.of(source), StandardCharsets.UTF_8))
                     .map(List::iterator)
                     .flatMap(YamlFileReader::read);
    }

    public static Result<StringMap> readString(String source) {
        return read(source.lines().iterator());
    }

    public static Result<StringMap> read(Iterator<String> source) {
        return new ReaderState().read(source);
    }
}
