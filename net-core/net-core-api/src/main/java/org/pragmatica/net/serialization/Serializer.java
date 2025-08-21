package org.pragmatica.net.serialization;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/// Basic serialization interface
public interface Serializer {
    default <T> byte[] encode(T object) {
        var outputStream = new ByteArrayOutputStream();
        write(outputStream, object);
        return outputStream.toByteArray();
    }

    <T> void write(ByteArrayOutputStream outputStream, T object);
}
