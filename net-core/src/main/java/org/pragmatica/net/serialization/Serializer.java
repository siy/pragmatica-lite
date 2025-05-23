package org.pragmatica.net.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/// Basic serialization interface
public interface Serializer {
    default <T> byte[] encode(T object) {
        var byteBuf = Unpooled.buffer();

        try {
            write(byteBuf, object);
            return byteBuf.array();
        } finally {
            byteBuf.release();
        }
    }

    <T> void write(ByteBuf byteBuf, T object);
}
