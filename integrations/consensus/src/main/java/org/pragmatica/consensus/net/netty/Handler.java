package org.pragmatica.consensus.net.netty;

import org.pragmatica.consensus.net.NetworkMessage.Hello;
import org.pragmatica.messaging.Message;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handler extends SimpleChannelInboundHandler<Message.Wired> {
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private final Consumer<Channel> peerConnected;
    private final Consumer<Channel> peerDisconnected;
    private final BiConsumer<Hello, Channel> helloHandler;
    private final Consumer<Message.Wired> messageHandler;

    public Handler(Consumer<Channel> peerConnected,
                   Consumer<Channel> peerDisconnected,
                   BiConsumer<Hello, Channel> helloHandler,
                   Consumer<Message.Wired> messageHandler) {
        this.peerConnected = peerConnected;
        this.peerDisconnected = peerDisconnected;
        this.helloHandler = helloHandler;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message.Wired msg) {
        log.trace("Received message: {}", msg);
        try{
            if (msg instanceof Hello hello) {
                helloHandler.accept(hello, ctx.channel());
            } else {
                if (msg.getClass()
                       .getSimpleName()
                       .contains("Invoke")) {
                    log.info("Handler routing InvokeMessage: {}",
                             msg.getClass()
                                .getName());
                }
                messageHandler.accept(msg);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", msg, e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("New channel active: {}",
                  ctx.channel()
                     .remoteAddress());
        peerConnected.accept(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Channel deactivated: {}",
                 ctx.channel()
                    .remoteAddress());
        peerDisconnected.accept(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error in channel", cause);
        ctx.close();
    }
}
