package org.pfj.http.server;

import org.pfj.lang.Promise;

@FunctionalInterface
public interface Handler<T> {
    Promise<T> handle(RequestContext request);
}
