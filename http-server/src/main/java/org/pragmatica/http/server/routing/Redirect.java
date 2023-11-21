package org.pragmatica.http.server.routing;

import io.netty.handler.codec.http.HttpResponseStatus;

public record Redirect(HttpResponseStatus status, String url) {
    //301
    public static Redirect movedPermanently(String url) {
        return new Redirect(HttpResponseStatus.MOVED_PERMANENTLY, url);
    }

    //302
    public static Redirect found(String url) {
        return new Redirect(HttpResponseStatus.FOUND, url);
    }

    //303
    public static Redirect seeOther(String url) {
        return new Redirect(HttpResponseStatus.SEE_OTHER, url);
    }

    //307
    public static Redirect temporary(String url) {
        return new Redirect(HttpResponseStatus.TEMPORARY_REDIRECT, url);
    }

    //308
    public static Redirect permanent(String url) {
        return new Redirect(HttpResponseStatus.PERMANENT_REDIRECT, url);
    }
}
