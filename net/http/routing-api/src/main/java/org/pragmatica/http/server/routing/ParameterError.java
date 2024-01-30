package org.pragmatica.http.server.routing;

import org.pragmatica.lang.Result.Cause;

public sealed interface ParameterError extends Cause {
    record MissingParameter(String message) implements ParameterError {}
}
