package org.pragmatica.net.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic deserialization interface
public interface Deserializer {
    default <T> T decode(byte[] bytes) {
        var byteBuf = Unpooled.wrappedBuffer(bytes);

        try {
            return read(byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    <T> T read(ByteBuf byteBuf);
}
