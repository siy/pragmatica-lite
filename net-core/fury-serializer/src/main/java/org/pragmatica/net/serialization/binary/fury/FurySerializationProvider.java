package org.pragmatica.net.serialization.binary.fury;

import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.SerializationProvider;
import org.pragmatica.net.serialization.Serializer;
import org.pragmatica.net.serialization.binary.ClassRegistrator;

public final class FurySerializationProvider implements SerializationProvider {
    
    @Override
    public String name() {
        return "fury";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("org.apache.fury.Fury");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public int priority() {
        return 100; // Higher priority than Kryo
    }
    
    @Override
    public <T> Serializer serializer(ClassRegistrator... registrators) {
        return FurySerializer.furySerializer(registrators);
    }
    
    @Override
    public <T> Deserializer deserializer(ClassRegistrator... registrators) {
        return FuryDeserializer.furyDeserializer(registrators);
    }
}