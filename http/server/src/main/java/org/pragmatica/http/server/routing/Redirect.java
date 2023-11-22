package org.pragmatica.http.server.routing;

import org.pragmatica.http.protocol.HttpStatus;

@SuppressWarnings("unused")
public record Redirect(HttpStatus status, String url) {
    //301
    public static Redirect movedPermanently(String url) {
        return new Redirect(HttpStatus.MOVED_PERMANENTLY, url);
    }

    //302
    public static Redirect found(String url) {
        return new Redirect(HttpStatus.FOUND, url);
    }

    //303
    public static Redirect seeOther(String url) {
        return new Redirect(HttpStatus.SEE_OTHER, url);
    }

    //307
    public static Redirect temporary(String url) {
        return new Redirect(HttpStatus.TEMPORARY_REDIRECT, url);
    }

    //308
    public static Redirect permanent(String url) {
        return new Redirect(HttpStatus.PERMANENT_REDIRECT, url);
    }
}
