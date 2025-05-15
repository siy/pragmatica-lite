package org.pragmatica.cluster.net.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.pragmatica.net.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(Decoder.class);

    private final Deserializer deserializer;

    public Decoder(Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            out.add(deserializer.read(in));
        } catch (Exception e) {
            log.error("Error decoding message", e);
            ctx.close();
        }
    }
}
