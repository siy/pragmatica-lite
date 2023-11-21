package org.pragmatica.http.server.util;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface HttpStatusHolder {
    HttpResponseStatus status();
}
