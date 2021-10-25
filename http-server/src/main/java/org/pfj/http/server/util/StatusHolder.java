package org.pfj.http.server.util;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface StatusHolder {
    HttpResponseStatus status();
}
