package org.pfj.http.server;

import io.netty.handler.codec.http.HttpMethod;
import org.pfj.lang.Result;

import java.util.function.Supplier;

public record Route<T>(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
    public Route(HttpMethod method, String path, Handler<T> handler, ContentType contentType) {
        this.method = method;
        this.path = sanitizePath(path);
        this.handler = handler;
        this.contentType = contentType;
    }

    private static String sanitizePath(String path) {
        var array = path.toCharArray();
        var builder = new StringBuilder();

        for(var i = 0; i < array.length; i++) {
            if (i == 0 && array[i] != '/') {
                builder.append('/');
            }
            builder.append(array[i]);
        }
        return builder.toString();
    }

    public static <T> Route<T> getText(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.GET, path, handler, ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> getText(String path, Supplier<Result<T>> supplier) {
        return new Route<>(HttpMethod.GET, path, __ -> supplier.get(), ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> postText(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.POST, path, handler, ContentType.TEXT_PLAIN);
    }

    public static <T> Route<T> getJson(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.GET, path, handler, ContentType.APPLICATION_JSON);
    }

    public static <T> Route<T> getJson(String path, Supplier<Result<T>> supplier) {
        return new Route<>(HttpMethod.GET, path, __ -> supplier.get(), ContentType.APPLICATION_JSON);
    }

    public static <T> Route<T> postJson(String path, Handler<T> handler) {
        return new Route<>(HttpMethod.POST, path, handler, ContentType.APPLICATION_JSON);
    }
}
