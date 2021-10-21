package org.pfj.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;

public final class Redirect {
    private final HttpResponseStatus status;
    private final String url;

    private Redirect(HttpResponseStatus status, String url) {
        this.status = status;
        this.url = url;
    }

    public HttpResponseStatus status() {
        return status;
    }

    public String url() {
        return url;
    }

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
