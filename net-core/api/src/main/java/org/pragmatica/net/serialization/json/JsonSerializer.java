package org.pragmatica.net.serialization.json;

import org.pragmatica.lang.Promise;

public interface JsonSerializer<T> {
    String toJson(T object);
    
    Promise<String> toJsonAsync(T object);
}