package org.pragmatica.net.transport.api;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ServiceLoader;

public interface TransportConfiguration {
    String name();

    boolean isAvailable();

    EventLoopGroup bossGroup();

    EventLoopGroup workerGroup();

    Class<? extends ServerSocketChannel> serverChannelClass();

    Class<? extends SocketChannel> clientChannelClass();
    Class<? extends DatagramChannel> datagramChannelClass();

    static TransportConfiguration transportConfiguration() {
        return TransportHolder.INSTANCE.transport;
    }

    default ServerBootstrap serverBootstrap() {
        return new ServerBootstrap().group(bossGroup(), workerGroup())
                                    .channel(serverChannelClass());
    }

    default Bootstrap clientBootstrap() {
        return new Bootstrap().group(workerGroup())
                              .channel(clientChannelClass());
    }

    default Bootstrap datagramBootstrap() {
        return new Bootstrap().group(workerGroup())
                              .channel(datagramChannelClass());
    }

    enum TransportHolder {
        INSTANCE;
        private final TransportConfiguration transport;

        TransportHolder() {
            record nioConfiguration(String name, boolean isAvailable, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                                    Class<? extends ServerSocketChannel> serverChannelClass,
                                    Class<? extends SocketChannel> clientChannelClass,
                                    Class<? extends DatagramChannel> datagramChannelClass)
                implements TransportConfiguration {}

            transport = ServiceLoader.load(TransportConfiguration.class)
                                     .stream()
                                     .map(ServiceLoader.Provider::get)
                                     .filter(TransportConfiguration::isAvailable)
                                     .findFirst()
                                     .orElseGet(() -> new nioConfiguration("NIO",
                                                                           true,
                                                                           new NioEventLoopGroup(1),
                                                                           new NioEventLoopGroup(),
                                                                           NioServerSocketChannel.class,
                                                                           NioSocketChannel.class,
                                                                           NioDatagramChannel.class));

        }
    }
}
