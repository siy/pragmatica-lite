package org.pragmatica.http.routing;

import org.pragmatica.lang.Cause;

/**
 * Error types for parameter parsing failures.
 */
public sealed interface ParameterError extends Cause {
    /**
     * Parameter was expected but not found in the request.
     */
    record MissingParameter(String parameterName) implements ParameterError {
        @Override
        public String message() {
            return "Missing required parameter: " + parameterName;
        }
    }

    /**
     * Parameter value could not be parsed to the expected type.
     */
    record InvalidParameter(String details) implements ParameterError {
        @Override
        public String message() {
            return details;
        }
    }

    /**
     * Path segment did not match the expected spacer value.
     */
    record PathMismatch(String expected, String actual) implements ParameterError {
        @Override
        public String message() {
            return "Path segment mismatch: expected '" + expected + "', got '" + actual + "'";
        }
    }
}
