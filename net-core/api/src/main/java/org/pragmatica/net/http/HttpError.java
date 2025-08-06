package org.pragmatica.net.http;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;

public final class HttpError implements Cause {
    private final int statusCode;
    private final String statusText;
    private final String body;
    private final Cause source;
    
    private HttpError(int statusCode, String statusText, String body, Cause source) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.body = body;
        this.source = source;
    }
    
    public static HttpError httpError(int statusCode, String statusText, String body) {
        return new HttpError(statusCode, statusText, body, null);
    }
    
    public static HttpError httpError(int statusCode, String statusText, String body, Cause source) {
        return new HttpError(statusCode, statusText, body, source);
    }
    
    public static <T> HttpError fromResponse(HttpResponse<T> response) {
        var bodyStr = response.body() != null ? response.body().toString() : "";
        return httpError(response.statusCode(), response.statusText(), bodyStr);
    }
    
    public int statusCode() {
        return statusCode;
    }
    
    public String statusText() {
        return statusText;
    }
    
    public String body() {
        return body;
    }
    
    @Override
    public String message() {
        return String.format("HTTP %d %s: %s", statusCode, statusText, body);
    }
    
    @Override
    public Option<Cause> source() {
        return Option.option(source);
    }
    
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    @Override
    public String toString() {
        return message();
    }
}