package org.pragmatica.net.http;

import org.pragmatica.lang.Result;

public interface HttpResponse<T> {
    
    int statusCode();
    
    default HttpStatusCode status() {
        return HttpStatusCode.fromCode(statusCode()).orElseThrow();
    }
    
    /// Get HttpError instance for this response 
    default HttpError error() {
        return HttpError.fromCode(statusCode(), statusText());
    }
    
    String statusText();
    
    HttpHeaders headers();
    
    T body();
    
    default boolean isSuccess() {
        return status().isSuccess();
    }
    
    default boolean isError() {
        return status().isError();
    }
    
    default boolean isClientError() {
        return status().isClientError();
    }
    
    default boolean isServerError() {
        return status().isServerError();
    }
    
    default Result<T> result() {
        return isSuccess() ? 
            Result.success(body()) : 
            Result.failure(HttpError.fromResponse(this));
    }
}