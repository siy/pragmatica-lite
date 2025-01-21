package org.pragmatica.dns;

import org.pragmatica.lang.Cause;

public sealed interface ResolverErrors extends Cause {
    record InvalidIpAddress(String message) implements ResolverErrors {}
    record ServerError(String message) implements ResolverErrors {}
    record RequestTimeout(String message) implements ResolverErrors {}
    record UnknownError(String message) implements ResolverErrors {}
    record UnknownDomain(String message) implements ResolverErrors {}
}
