package org.pragmatica.net.serialization;

import java.nio.ByteBuffer;

/// Basic deserialization interface
public interface Deserializer {
    default <T> T decode(byte[] bytes) {
        return read(ByteBuffer.wrap(bytes));
    }

    <T> T read(ByteBuffer buffer);
}
