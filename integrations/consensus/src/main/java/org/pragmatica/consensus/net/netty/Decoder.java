package org.pragmatica.consensus.net.netty;

import org.pragmatica.messaging.Message;
import org.pragmatica.serialization.Deserializer;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger(Decoder.class);

    private final Deserializer deserializer;

    public Decoder(Deserializer deserializer) {
        this.deserializer = deserializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try{
            var object = deserializer.read(in);
            if (object instanceof Message.Wired) {
                if (object.getClass()
                          .getSimpleName()
                          .contains("Invoke")) {
                    log.info("Decoder decoded InvokeMessage: {}",
                             object.getClass()
                                   .getName());
                }
                out.add(object);
            } else {
                log.error("Attempt to decode non-Wired object: {}", object);
            }
        } catch (Exception e) {
            log.error("Error decoding message", e);
            ctx.close();
        }
    }
}
