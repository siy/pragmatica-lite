package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

public sealed interface ConfigurationError extends Result.Cause {
    record InputIsMissing(String message) implements ConfigurationError {}

    record InvalidCommandLineParameter(String message) implements ConfigurationError {}

    static ConfigurationError invalidParameter(String[] argument) {
        if (argument.length == 0) {
            return new InvalidCommandLineParameter("Invalid argument: ");
        }

        return new InvalidCommandLineParameter(STR."Invalid argument: \{argument[0]}");
    }
}
