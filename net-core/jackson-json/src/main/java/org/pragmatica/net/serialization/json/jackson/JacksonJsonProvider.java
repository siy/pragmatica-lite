package org.pragmatica.net.serialization.json.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.TypeToken;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.net.serialization.json.JsonCodec;
import org.pragmatica.net.serialization.json.JsonDeserializer;
import org.pragmatica.net.serialization.json.JsonProvider;
import org.pragmatica.net.serialization.json.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JacksonJsonProvider implements JsonProvider {
    private static final Logger log = LoggerFactory.getLogger(JacksonJsonProvider.class);
    
    private final ObjectMapper objectMapper;
    
    public JacksonJsonProvider() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public JacksonJsonProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String name() {
        return "jackson";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public int priority() {
        return 100; // Default priority
    }
    
    @Override
    public <T> JsonCodec<T> codec(Class<T> type) {
        return new JacksonJsonCodec<>(objectMapper, type);
    }
    
    @Override
    public <T> JsonCodec<T> codec(TypeToken<T> typeToken) {
        return new JacksonJsonCodec<>(objectMapper, typeToken);
    }
    
    @Override
    public JsonCodec<Object> genericCodec() {
        return codec(Object.class);
    }
    
    private static final class JacksonJsonCodec<T> implements JsonCodec<T> {
        private final ObjectMapper objectMapper;
        private final JavaType javaType;
        
        JacksonJsonCodec(ObjectMapper objectMapper, Class<T> type) {
            this.objectMapper = objectMapper;
            this.javaType = TypeFactory.defaultInstance().constructType(type);
        }
        
        JacksonJsonCodec(ObjectMapper objectMapper, TypeToken<T> typeToken) {
            this.objectMapper = objectMapper;
            this.javaType = TypeFactory.defaultInstance().constructType(typeToken.token());
        }
        
        
        // JsonSerializer methods
        @Override
        public String toJson(T object) {
            try {
                return objectMapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                log.error("Error serializing object to JSON", e);
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public Promise<String> toJsonAsync(T object) {
            return Promise.lift(() -> toJson(object));
        }
        
        // JsonDeserializer methods
        @Override
        public T fromJson(String json, Class<T> type) {
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing JSON to object", e);
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public T fromJson(String json, TypeToken<T> typeToken) {
            try {
                JavaType jacksonType = TypeFactory.defaultInstance().constructType(typeToken.token());
                return objectMapper.readValue(json, jacksonType);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing JSON to object with TypeToken", e);
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public Promise<T> fromJsonAsync(String json, Class<T> type) {
            return Promise.lift(() -> fromJson(json, type));
        }
        
        @Override
        public Promise<T> fromJsonAsync(String json, TypeToken<T> typeToken) {
            return Promise.lift(() -> fromJson(json, typeToken));
        }
        
        @Override
        public Result<T> tryFromJson(String json, Class<T> type) {
            try {
                return Result.success(fromJson(json, type));
            } catch (Exception e) {
                return Result.failure(Causes.fromThrowable(e));
            }
        }
        
        @Override
        public Result<T> tryFromJson(String json, TypeToken<T> typeToken) {
            try {
                return Result.success(fromJson(json, typeToken));
            } catch (Exception e) {
                return Result.failure(Causes.fromThrowable(e));
            }
        }
    }
}