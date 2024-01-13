package org.pragmatica.config.format.yaml;

import com.google.common.io.Files;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class YamlFileReader {

    private static final char TAB_CHAR = '\t';
    private static final char SPACE_CHAR = ' ';
    private static final Set<Character> SPACE_CHARACTERS = Set.of(TAB_CHAR, SPACE_CHAR);
    private static final String COLON_CHAR = ":";
    private static final String DOT_CHAR = ".";
    private static final String START_COMMENT_CHAR = "#";
    private static final String KEY_PATTERN = "[a-zA-Z0-9._-]+";
    private static final String DASH_CHAR = "-";
    private static final String DOUBLE_QUOTE_CHAR = "\"";
    private static final String SINGLE_QUOTE_CHAR = "'";
    private static final String EMPTY_STRING = "";

    private static class ReaderState {
        int lineIndex = 0;
        int lastSpaceCount = 0;
        String line;
        String lastParentKey = null;
        String lastPropertyKey = null;
        HashMap<String, String> map = new HashMap<>();
        TreeMap<Integer, YamlNode> nodes = new TreeMap<>();

        private String getClearValue(String rawValue) {
            String clearValue = rawValue.trim();
            int commentIndex = clearValue.indexOf(START_COMMENT_CHAR);
            if (commentIndex >= 0) {
                clearValue = clearValue.substring(0, commentIndex).trim();
            }
            if (clearValue.startsWith(DOUBLE_QUOTE_CHAR)
                && clearValue.endsWith(DOUBLE_QUOTE_CHAR)
            ) {
                if (clearValue.length() > 2) {
                    clearValue = clearValue.substring(1, clearValue.length() - 1);
                } else {
                    return EMPTY_STRING;
                }
            } else if (clearValue.startsWith(SINGLE_QUOTE_CHAR)
                       && clearValue.endsWith(SINGLE_QUOTE_CHAR)
            ) {
                if (clearValue.length() > 2) {
                    clearValue = clearValue.substring(1, clearValue.length() - 1);
                } else {
                    return EMPTY_STRING;
                }
            } else if (clearValue.startsWith(">-")) {
                if (clearValue.length() > 2) {
                    clearValue = clearValue.substring(2).trim();
                } else {
                    clearValue = EMPTY_STRING;
                }
            } else if (clearValue.startsWith("|")) {
                if (clearValue.length() > 1) {
                    clearValue = clearValue.substring(1).trim();
                } else {
                    clearValue = EMPTY_STRING;
                }
            } else if (clearValue.startsWith(">")) {
                if (clearValue.length() > 1) {
                    clearValue = clearValue.substring(1).trim();
                } else {
                    clearValue = EMPTY_STRING;
                }
            } else {
                clearValue = clearValue.trim();
            }
            return clearValue;
        }

        protected int countStartedSpace(String line) {
            int count = 0;
            for (int i = 0; ; ++i) {
                char ch = line.charAt(i);
                if (!SPACE_CHARACTERS.contains(ch)) {
                    break;
                }
                if (ch == TAB_CHAR) {
                    count += 4;
                } else {
                    count += 1;
                }
            }
            return count;
        }

        Result<StringMap> read(Iterator<String> lines) {
            while (lines.hasNext()) {
                line = lines.next();
                ++lineIndex;

                var lineTrim = line.trim();

                if (lineTrim.isEmpty()) {
                    continue;
                }

                if (lineTrim.startsWith(START_COMMENT_CHAR)) {
                    continue;
                }

                if (!lineTrim.contains(COLON_CHAR)) {
                    if (lastPropertyKey == null) {
                        return YamlParseError.invalidSyntax("", lineIndex, line).result();
                    } else {
                        var lastValue = map.getOrDefault(lastPropertyKey, EMPTY_STRING).trim();

                        if (lineTrim.startsWith(DASH_CHAR)) {
                            if (lineTrim.length() > 1) {
                                var value = lineTrim.substring(1).trim();
                                lastValue = lastValue.isEmpty() ? value : (STR."\{lastValue},\{value}");
                            }
                        } else {
                            lastValue = lastValue.isEmpty() ? lineTrim : (STR."\{lastValue} \{lineTrim}");
                        }
                        map.put(lastPropertyKey, lastValue);
                        continue;
                    }
                }

                var keyValue = lineTrim.split(COLON_CHAR, 2);

                var keyTrim = keyValue[0].trim();
                if (keyTrim.isEmpty()) {
                    return YamlParseError.invalidSyntax("missing key", lineIndex, line).result();
                }

                String clearKey = getClearKey(keyTrim);

                if (clearKey == null) {
                    int spaceCount = countStartedSpace(line);
                    if (lastPropertyKey == null || spaceCount <= lastSpaceCount) {
                        return YamlParseError.invalidSyntax(STR."invalid key \{keyTrim}", lineIndex, line).result();
                    }
                    String lastValue = map.getOrDefault(lastPropertyKey, EMPTY_STRING);
                    lastValue = lastValue.isEmpty() ? lineTrim : (STR."\{lastValue} \{lineTrim}");
                    map.put(lastPropertyKey, lastValue.trim());
                    continue;
                }

                int spaceCount = countStartedSpace(line);
                Map.Entry<Integer, YamlNode> parentEntry = nodes.lowerEntry(spaceCount);
                YamlNode parentNode = null;

                if (parentEntry != null) {
                    parentNode = parentEntry.getValue();
                }

                if (keyValue[1].isBlank()) {
                    if (parentNode != null) {
                        lastParentKey = parentNode.propertyName + DOT_CHAR + keyTrim;
                    } else {
                        lastParentKey = clearKey;
                    }
                    nodes.put(spaceCount, new YamlNode(lastParentKey));
                    lastPropertyKey = lastParentKey;
                } else {
                    if (parentNode == null) {
                        lastParentKey = null;
                    }
                    String fullKey = clearKey;
                    if (lastParentKey != null) {
                        fullKey = lastParentKey + DOT_CHAR + clearKey;
                    }
                    map.put(fullKey, getClearValue(keyValue[1]));
                    lastPropertyKey = fullKey;
                }
                lastSpaceCount = spaceCount;
            }
            return Result.success(() -> map);
        }

        private String getClearKey(String rawKey) {
            String clearKey = rawKey;

            if (clearKey.startsWith(DASH_CHAR)) {
                clearKey = clearKey.substring(1).trim();
            }

            if (clearKey.isEmpty()) {
                return null;
            }

            if (!clearKey.matches(KEY_PATTERN)) {
                return null;
            }

            return clearKey;
        }
    }

    public static Result<StringMap> readFile(String source) {
        return Result.lift(Causes::fromThrowable, () -> Files.readLines(new File(source), StandardCharsets.UTF_8))
                     .map(List::iterator)
                     .flatMap(YamlFileReader::read);
    }

    public static Result<StringMap> readString(String source) {
        return read(source.lines().iterator());
    }

    public static Result<StringMap> read(Iterator<String> source) {
        return new ReaderState().read(source);
    }

    private record YamlNode(String propertyName) {}

    ;
}