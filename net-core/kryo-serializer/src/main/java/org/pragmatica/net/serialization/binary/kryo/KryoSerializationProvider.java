package org.pragmatica.net.serialization.binary.kryo;

import org.pragmatica.net.serialization.Deserializer;
import org.pragmatica.net.serialization.SerializationProvider;
import org.pragmatica.net.serialization.Serializer;
import org.pragmatica.net.serialization.binary.ClassRegistrator;

public final class KryoSerializationProvider implements SerializationProvider {
    
    @Override
    public String name() {
        return "kryo";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.esotericsoftware.kryo.Kryo");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public int priority() {
        return 50; // Lower priority than Fury
    }
    
    @Override
    public <T> Serializer serializer(ClassRegistrator... registrators) {
        return KryoSerializer.kryoSerializer(registrators);
    }
    
    @Override
    public <T> Deserializer deserializer(ClassRegistrator... registrators) {
        return KryoDeserializer.kryoDeserializer(registrators);
    }
}