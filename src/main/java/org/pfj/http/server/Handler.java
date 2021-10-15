package org.pfj.http.server;

import io.netty.handler.codec.http.FullHttpRequest;
import org.pfj.lang.Result;

@FunctionalInterface
public interface Handler<T> {
    Result<T> handle(FullHttpRequest request);
}
