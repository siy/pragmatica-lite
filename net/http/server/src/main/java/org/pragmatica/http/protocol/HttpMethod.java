package org.pragmatica.http.protocol;

import org.pragmatica.http.server.error.WebError;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

import java.util.IdentityHashMap;

public enum HttpMethod {
    CONNECT,
    DELETE,
    GET,
    HEAD,
    OPTIONS,
    PATCH,
    POST,
    PUT,
    TRACE,
    ;

    private static final Result<HttpMethod> NOT_ALLOWED = WebError.from(HttpStatus.METHOD_NOT_ALLOWED, Causes.cause("Method not allowed")).result();

    public static Result<HttpMethod> fromString(String method) {
        int length = method.length();

        if (length < 3) {
            return NOT_ALLOWED;
        }

        switch (method.charAt(0)) {
            case 'C' -> {
                if (length == 7 && method.equals("CONNECT")) {
                    return Result.success(CONNECT);
                }
            }
            case 'D' -> {
                if (length == 6 && method.equals("DELETE")) {
                    return Result.success(DELETE);
                }
            }
            case 'G' -> {
                if (length == 3 && method.equals("GET")) {
                    return Result.success(GET);
                }
            }
            case 'H' -> {
                if (length == 4 && method.equals("HEAD")) {
                    return Result.success(HEAD);
                }
            }
            case 'O' -> {
                if (length == 7 && method.equals("OPTIONS")) {
                    return Result.success(OPTIONS);
                }
            }
            case 'P' -> {
                if (length == 5 && method.equals("PATCH")) {
                    return Result.success(PATCH);
                }
                if (length == 4 && method.equals("POST")) {
                    return Result.success(POST);
                }
                if (length == 3 && method.equals("PUT")) {
                    return Result.success(PUT);
                }
            }
            case 'T' -> {
                if (length == 5 && method.equals("TRACE")) {
                    return Result.success(TRACE);
                }
            }
        }

        return NOT_ALLOWED;
    }

    private static final IdentityHashMap<io.netty.handler.codec.http.HttpMethod, HttpMethod> METHODS = new IdentityHashMap<>();

    static {
        METHODS.put(io.netty.handler.codec.http.HttpMethod.CONNECT, CONNECT);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.DELETE, DELETE);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.GET, GET);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.HEAD, HEAD);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.OPTIONS, OPTIONS);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.PATCH, PATCH);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.POST, POST);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.PUT, PUT);
        METHODS.put(io.netty.handler.codec.http.HttpMethod.TRACE, TRACE);
    }

    public static HttpMethod from(io.netty.handler.codec.http.HttpMethod method) {
        return METHODS.get(method);
    }
}
