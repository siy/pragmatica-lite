package org.pragmatica.net.http;

import org.pragmatica.lang.Result;

public interface HttpResponse<T> {
    
    int statusCode();
    
    default Result<HttpStatusCode> status() {
        return HttpStatusCode.fromCode(statusCode());
    }
    
    /// Get HttpError instance for this response 
    default HttpError error() {
        return HttpError.fromCode(statusCode(), statusText());
    }
    
    String statusText();
    
    HttpHeaders headers();
    
    T body();
    
    default boolean isSuccess() {
        return status().map(HttpStatusCode::isSuccess).orElse(false);
    }
    
    default boolean isError() {
        return status().map(s -> s.isClientError() || s.isServerError()).orElse(true);
    }
    
    default boolean isClientError() {
        return status().map(HttpStatusCode::isClientError).orElse(false);
    }
    
    default boolean isServerError() {
        return status().map(HttpStatusCode::isServerError).orElse(false);
    }
    
    default Result<T> result() {
        return isSuccess() ? 
            Result.success(body()) : 
            Result.failure(HttpError.fromResponse(this));
    }
}