package org.pragmatica.net.http;

import org.pragmatica.lang.Result;

public interface HttpResponse<T> {
    
    /// Returns the HTTP status code as enum
    HttpStatus status();
    
    HttpHeaders headers();
    
    Result<T> result();
    
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
}