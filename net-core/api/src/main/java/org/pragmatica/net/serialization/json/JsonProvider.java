package org.pragmatica.net.serialization.json;

import org.pragmatica.lang.type.TypeToken;

public interface JsonProvider {
    String name();
    
    boolean isAvailable();
    
    int priority();
    
    <T> JsonCodec<T> codec(Class<T> type);
    
    default <T> JsonCodec<T> codec(TypeToken<T> typeToken) {
        // Default implementation that falls back to raw type - providers should override for full TypeToken support
        return codec((Class<T>) typeToken.rawType());
    }
    
    JsonCodec<Object> genericCodec();
}