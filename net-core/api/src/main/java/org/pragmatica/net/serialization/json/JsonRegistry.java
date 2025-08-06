package org.pragmatica.net.serialization.json;

import org.pragmatica.lang.type.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public final class JsonRegistry {
    private static final Logger log = LoggerFactory.getLogger(JsonRegistry.class);
    
    private static volatile JsonProvider defaultProvider;
    
    public static JsonProvider provider() {
        if (defaultProvider == null) {
            synchronized (JsonRegistry.class) {
                if (defaultProvider == null) {
                    defaultProvider = findBestProvider("jackson");
                }
            }
        }
        return defaultProvider;
    }
    
    public static JsonProvider jsonProvider(String preferred) {
        return findBestProvider(preferred);
    }
    
    public static <T> JsonCodec<T> codec(Class<T> type) {
        return provider().codec(type);
    }
    
    public static <T> JsonCodec<T> codec(TypeToken<T> typeToken) {
        return provider().codec(typeToken);
    }
    
    public static JsonCodec<Object> genericCodec() {
        return provider().genericCodec();
    }
    
    private static JsonProvider findBestProvider(String preferred) {
        var serviceLoader = ServiceLoader.load(JsonProvider.class);
        var providers = StreamSupport.stream(serviceLoader.spliterator(), false).toList();
        
        if (providers.isEmpty()) {
            throw new IllegalStateException("No JSON providers found via ServiceLoader");
        }
        
        // First try to find preferred provider if it's available
        Optional<JsonProvider> preferredProvider = providers.stream()
            .filter(p -> p.name().equalsIgnoreCase(preferred) && p.isAvailable())
            .findFirst();
            
        if (preferredProvider.isPresent()) {
            log.info("Using preferred JSON provider: {}", preferredProvider.get().name());
            return preferredProvider.get();
        }
        
        // Fall back to highest priority available provider
        JsonProvider fallback = providers.stream()
            .filter(JsonProvider::isAvailable)
            .max((p1, p2) -> Integer.compare(p1.priority(), p2.priority()))
            .orElseThrow(() -> new IllegalStateException("No JSON providers available"));
            
        log.info("Using fallback JSON provider: {} (preferred {} not available)", 
                 fallback.name(), preferred);
        return fallback;
    }
}