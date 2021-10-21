package org.pfj.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

public interface StatusHolder {
    HttpResponseStatus status();
}
