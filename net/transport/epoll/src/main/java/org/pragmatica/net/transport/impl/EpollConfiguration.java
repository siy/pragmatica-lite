package org.pragmatica.net.transport.impl;


import com.google.auto.service.AutoService;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.pragmatica.net.transport.api.TransportConfiguration;

@SuppressWarnings("unused") //used via ServiceLoader
@AutoService(TransportConfiguration.class)
public class EpollConfiguration implements TransportConfiguration {
    @Override
    public String name() {
        return "epoll";
    }

    @Override
    public boolean isAvailable() {
        return Epoll.isAvailable();
    }

    @Override
    public EventLoopGroup bossGroup() {
        return new EpollEventLoopGroup(1);
    }

    @Override
    public EventLoopGroup workerGroup() {
        return new EpollEventLoopGroup();
    }

    @Override
    public Class<? extends ServerSocketChannel> serverChannelClass() {
        return EpollServerSocketChannel.class;
    }

    @Override
    public Class<? extends SocketChannel> clientChannelClass() {
        return EpollSocketChannel.class;
    }

    @Override
    public Class<? extends DatagramChannel> datagramChannelClass() {
        return EpollDatagramChannel.class;
    }
}
