package org.pragmatica.http.server.routing;

import org.pragmatica.lang.Promise;

@FunctionalInterface
public interface Handler<T> {
    Promise<T> handle(RequestContext request);
}
