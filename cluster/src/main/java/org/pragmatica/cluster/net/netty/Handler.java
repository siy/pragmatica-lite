package org.pragmatica.cluster.net.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.pragmatica.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class Handler extends SimpleChannelInboundHandler<Message.Wired> {
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private final Consumer<Channel> peerConnected;
    private final Consumer<Channel> peerDisconnected;
    private final Consumer<Message.Wired> messageHandler;

    public Handler(Consumer<Channel> peerConnected, Consumer<Channel> peerDisconnected, Consumer<Message.Wired> messageHandler) {
        this.peerConnected = peerConnected;
        this.peerDisconnected = peerDisconnected;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message.Wired msg) {
        log.trace("Received message: {}", msg);
        
        try {
            messageHandler.accept(msg);
        } catch (Exception e) {
            log.error("Error handling message: {}", msg, e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("New channel active: {}", ctx.channel().remoteAddress());

        peerConnected.accept(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("Channel deactivated: {}", ctx.channel().remoteAddress());

        peerDisconnected.accept(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error in channel", cause);
        ctx.close();
    }
}
