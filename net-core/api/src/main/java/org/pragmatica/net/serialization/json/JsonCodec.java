package org.pragmatica.net.serialization.json;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;

import java.util.List;
import java.util.Map;

public interface JsonCodec<T> extends JsonSerializer<T>, JsonDeserializer<T> {
    
    default Promise<String> serializeAsync(T object) {
        return toJsonAsync(object);
    }
    
    default Promise<T> deserializeAsync(String json, Class<T> type) {
        return fromJsonAsync(json, type);
    }
    
    default Promise<T> deserializeAsync(String json, TypeToken<T> typeToken) {
        return fromJsonAsync(json, typeToken);
    }
    
    
    static <T> JsonCodec<T> of(Class<T> type) {
        return JsonRegistry.provider().codec(type);
    }
    
    static <T> JsonCodec<T> of(TypeToken<T> typeToken) {
        return JsonRegistry.provider().codec(typeToken);
    }
    
    // Convenience methods for common generic types
    static <T> JsonCodec<List<T>> listOf(Class<T> elementType) {
        return of(new TypeToken<List<T>>(){});
    }
    
    static <T> JsonCodec<List<T>> listOf(TypeToken<T> elementType) {
        return of(new TypeToken<List<T>>(){});
    }
    
    static <K, V> JsonCodec<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
        return of(new TypeToken<Map<K, V>>(){});
    }
    
    static <K, V> JsonCodec<Map<K, V>> mapOf(TypeToken<K> keyType, TypeToken<V> valueType) {
        return of(new TypeToken<Map<K, V>>(){});
    }
}