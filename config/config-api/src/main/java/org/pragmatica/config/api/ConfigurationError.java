package org.pragmatica.config.api;

import org.pragmatica.lang.Result;

public sealed interface ConfigurationError extends Result.Cause {
    record InputIsMissing(String message) implements ConfigurationError {}
}
