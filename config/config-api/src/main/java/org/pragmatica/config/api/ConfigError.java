package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

public sealed interface ConfigError extends Result.Cause {
    record InputIsMissing(String message) implements ConfigError {}

    record InvalidCommandLineParameter(String message) implements ConfigError {}

    static ConfigError invalidParameter(String[] argument) {
        if (argument.length == 0) {
            return new InvalidCommandLineParameter("Invalid argument: ");
        }

        return new InvalidCommandLineParameter(STR."Invalid argument: \{argument[0]}");
    }
}
