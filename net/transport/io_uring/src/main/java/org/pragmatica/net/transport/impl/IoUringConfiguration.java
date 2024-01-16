package org.pragmatica.net.transport.impl;


import com.google.auto.service.AutoService;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.incubator.channel.uring.*;
import org.pragmatica.net.transport.api.TransportConfiguration;

@SuppressWarnings("unused")
@AutoService(TransportConfiguration.class)
public class IoUringConfiguration implements TransportConfiguration {
    @Override
    public String name() {
        return "io_uring";
    }

    @Override
    public boolean isAvailable() {
        return IOUring.isAvailable();
    }

    @Override
    public EventLoopGroup bossGroup() {
        return new IOUringEventLoopGroup(1);
    }

    @Override
    public EventLoopGroup workerGroup() {
        return new IOUringEventLoopGroup();
    }

    @Override
    public Class<? extends ServerSocketChannel> serverChannelClass() {
        return IOUringServerSocketChannel.class;
    }

    @Override
    public Class<? extends SocketChannel> clientChannelClass() {
        return IOUringSocketChannel.class;
    }

    @Override
    public Class<? extends DatagramChannel> datagramChannelClass() {
        return IOUringDatagramChannel.class;
    }
}
