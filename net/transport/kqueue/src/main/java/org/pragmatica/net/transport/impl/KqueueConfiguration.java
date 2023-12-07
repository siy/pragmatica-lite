package org.pragmatica.net.transport.impl;


import com.google.auto.service.AutoService;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.pragmatica.net.transport.api.TransportConfiguration;

@SuppressWarnings("unused")
@AutoService(TransportConfiguration.class)
public class KqueueConfiguration implements TransportConfiguration {
    @Override
    public String name() {
        return "kqueue";
    }

    @Override
    public boolean isAvailable() {
        return KQueue.isAvailable();
    }

    @Override
    public EventLoopGroup bossGroup() {
        return new KQueueEventLoopGroup(1);
    }

    @Override
    public EventLoopGroup workerGroup() {
        return new KQueueEventLoopGroup();
    }

    @Override
    public Class<? extends ServerSocketChannel> serverChannelClass() {
        return KQueueServerSocketChannel.class;
    }

    @Override
    public Class<? extends SocketChannel> clientChannelClass() {
        return KQueueSocketChannel.class;
    }

    @Override
    public Class<? extends DatagramChannel> datagramChannelClass() {
        return KQueueDatagramChannel.class;
    }
}
