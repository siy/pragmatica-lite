package org.pragmatica.net.serialization;

import org.pragmatica.net.serialization.binary.ClassRegistrator;

public interface SerializationProvider {
    String name();
    
    boolean isAvailable();
    
    int priority();
    
    <T> Serializer serializer(ClassRegistrator... registrators);
    
    <T> Deserializer deserializer(ClassRegistrator... registrators);
}