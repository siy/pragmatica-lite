package org.pragmatica.config.format.conf;

import org.pragmatica.config.api.DataConversionError;
import org.pragmatica.config.api.StringMap;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.HashMap;
import java.util.Map;

//TODO: tests
public class ConfParser {
    private static class ConfParserState {
        private final Map<String, String> values = new HashMap<>();
        private String currentSection = "";

        Result<Unit> process(String line) {
            if (line.startsWith("[")) {
                if (!line.endsWith("]")) {
                    return new DataConversionError.InvalidInput(STR."The line \{line} does not represent a valid section name")
                        .result();
                }

                currentSection = line.substring(1, line.length() - 1).trim();
                return Result.unitResult();
            }

            var parts = line.split("=", 2);

            if (parts.length != 2) {
                return new DataConversionError.InvalidInput(STR."The line \{line} does not represent a valid key/value pair")
                    .result();
            }

            var key = currentSection.isBlank()
                      ? parts[0].strip()
                      : STR."\{currentSection}.\{parts[0].strip()}";

            values.put(key, parts[1].strip());

            return Result.unitResult();
        }
    }

    public static Result<StringMap> parse(String content) {
        var state = new ConfParserState();

        return content.lines()
                      .map(state::process)
                      .filter(Result::isFailure)
                      .findFirst()
                      .orElseGet(Result::unitResult)
                      .map(_ -> () -> state.values);
    }
}
