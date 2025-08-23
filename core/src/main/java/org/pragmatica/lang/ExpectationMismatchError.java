package org.pragmatica.lang;

/// Dedicated [Error] for `expect()`.
public class ExpectationMismatchError extends Error {
    public ExpectationMismatchError(String message) {
        super(message);
    }
}
