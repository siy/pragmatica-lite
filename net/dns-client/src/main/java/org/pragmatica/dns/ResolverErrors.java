package org.pragmatica.dns;

import org.pragmatica.lang.Result;

public sealed interface ResolverErrors extends Result.Cause {
    record InvalidIpAddress(String message) implements ResolverErrors {}

    record UnknownError(String message) implements Result.Cause {}
}