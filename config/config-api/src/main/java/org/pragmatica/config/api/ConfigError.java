package org.pragmatica.config.api;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.utils.Causes;

import static org.pragmatica.lang.Option.some;

public sealed interface ConfigError extends Cause {
    record InputIsMissing(String message) implements ConfigError {}
    record InvalidCommandLineParameter(String message) implements ConfigError {}
    record IOError(String message, Option<Cause> source) implements ConfigError {}

    static ConfigError invalidParameter(String[] argument) {
        if (argument.length == 0) {
            return new InvalidCommandLineParameter("Invalid argument: ");
        }

        return new InvalidCommandLineParameter("Invalid argument: " + argument[0]);
    }

    static ConfigError ioError(Throwable throwable) {
        return new IOError(throwable.getClass().getSimpleName() + ": " + throwable.getMessage(), some(Causes.fromThrowable(throwable)));
    }
}
