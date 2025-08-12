package org.pragmatica.net.http;

import org.pragmatica.lang.Result;

public interface HttpResponse<T> {
    
    /// Returns the HTTP status code as enum
    HttpStatusCode status();
    
    HttpHeaders headers();
    
    T body();
    
    default boolean isSuccess() {
        return status().isSuccess();
    }
    
    default boolean isError() {
        return status().isClientError() || status().isServerError();
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