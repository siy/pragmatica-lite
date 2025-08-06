package org.pragmatica.net.serialization;

import org.pragmatica.net.serialization.binary.ClassRegistrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public final class SerializationRegistry {
    private static final Logger log = LoggerFactory.getLogger(SerializationRegistry.class);
    
    private static volatile SerializationProvider defaultBinaryProvider;
    
    public static SerializationProvider binary() {
        if (defaultBinaryProvider == null) {
            synchronized (SerializationRegistry.class) {
                if (defaultBinaryProvider == null) {
                    defaultBinaryProvider = findBestProvider("fury");
                }
            }
        }
        return defaultBinaryProvider;
    }
    
    public static SerializationProvider binaryProvider(String preferred) {
        return findBestProvider(preferred);
    }
    
    public static Serializer binarySerializer(ClassRegistrator... registrators) {
        return binary().serializer(registrators);
    }
    
    public static Deserializer binaryDeserializer(ClassRegistrator... registrators) {
        return binary().deserializer(registrators);
    }
    
    private static SerializationProvider findBestProvider(String preferred) {
        var serviceLoader = ServiceLoader.load(SerializationProvider.class);
        var providers = StreamSupport.stream(serviceLoader.spliterator(), false).toList();
        
        if (providers.isEmpty()) {
            throw new IllegalStateException("No serialization providers found via ServiceLoader");
        }
        
        // First try to find preferred provider if it's available
        Optional<SerializationProvider> preferredProvider = providers.stream()
            .filter(p -> p.name().equalsIgnoreCase(preferred) && p.isAvailable())
            .findFirst();
            
        if (preferredProvider.isPresent()) {
            log.info("Using preferred serialization provider: {}", preferredProvider.get().name());
            return preferredProvider.get();
        }
        
        // Fall back to highest priority available provider
        SerializationProvider fallback = providers.stream()
            .filter(SerializationProvider::isAvailable)
            .max((p1, p2) -> Integer.compare(p1.priority(), p2.priority()))
            .orElseThrow(() -> new IllegalStateException("No serialization providers available"));
            
        log.info("Using fallback serialization provider: {} (preferred {} not available)", 
                 fallback.name(), preferred);
        return fallback;
    }
}