package org.pragmatica.net.serialization.json;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

public interface JsonDeserializer<T> {
    T fromJson(String json, Class<T> type);
    
    T fromJson(String json, TypeToken<T> typeToken);
    
    Promise<T> fromJsonAsync(String json, Class<T> type);
    
    Promise<T> fromJsonAsync(String json, TypeToken<T> typeToken);
    
    Result<T> tryFromJson(String json, Class<T> type);
    
    Result<T> tryFromJson(String json, TypeToken<T> typeToken);
}