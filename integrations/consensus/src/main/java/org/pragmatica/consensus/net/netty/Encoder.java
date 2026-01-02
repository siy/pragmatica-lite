package org.pragmatica.consensus.net.netty;

import org.pragmatica.messaging.Message;
import org.pragmatica.serialization.Serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Encoder extends MessageToByteEncoder<Message.Wired> {
    private static final Logger log = LoggerFactory.getLogger(Encoder.class);

    private final Serializer serializer;

    public Encoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message.Wired msg, ByteBuf out) {
        try{
            serializer.write(out, msg);
        } catch (Exception e) {
            log.error("Error encoding message", e);
            ctx.close();
        }
    }
}
