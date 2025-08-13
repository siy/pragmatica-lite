package org.pragmatica.net.serialization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServiceLoaderTest {
    
    @Test
    void shouldDiscoverSerializationProviders() {
        var registry = SerializationRegistry.binary();
        
        assertNotNull(registry);
        assertThat(registry.name()).isIn("fury", "kryo");
        assertThat(registry.isAvailable()).isTrue();
    }
    
    @Test
    void shouldPreferFuryOverKryo() {
        var furyProvider = SerializationRegistry.binaryProvider("fury");
        var kryoProvider = SerializationRegistry.binaryProvider("kryo");
        
        if (furyProvider.isAvailable() && kryoProvider.isAvailable()) {
            assertThat(furyProvider.priority()).isGreaterThan(kryoProvider.priority());
        }
    }
    
    @Test
    void shouldCreateSerializersAndDeserializers() {
        var provider = SerializationRegistry.binary();
        
        var serializer = provider.serializer();
        var deserializer = provider.deserializer();
        
        assertNotNull(serializer);
        assertNotNull(deserializer);
    }
}