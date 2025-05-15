package org.pragmatica.net.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic deserialization interface
public interface Deserializer {
    default <T> T decode(byte[] bytes) {
        return read(Unpooled.wrappedBuffer(bytes));
    }

    <T> T read(ByteBuf byteBuf);
}
