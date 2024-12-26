package org.pragmatica.config.format.yaml;

import org.pragmatica.lang.Cause;

public sealed interface YamlParseError extends Cause {
    record InvalidSyntaxError(String message) implements YamlParseError {}

    static InvalidSyntaxError invalidSyntax(String message, int lineIndex, String line) {
        var description = message.isBlank() ? "" : "(" + message + ") ";
        return new InvalidSyntaxError("Invalid syntax " + description + "at line " + lineIndex + ": " + line);
    }
}
