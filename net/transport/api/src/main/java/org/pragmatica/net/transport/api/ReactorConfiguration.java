package org.pragmatica.net.transport.api;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ServiceLoader;

public interface ReactorConfiguration {
    String name();

    boolean isAvailable();

    EventLoopGroup bossGroup();

    EventLoopGroup workerGroup();

    Class<? extends ServerSocketChannel> serverChannelClass();

    Class<? extends SocketChannel> clientChannelClass();

    static ReactorConfiguration reactorConfiguration() {
        return TransportHolder.INSTANCE.transport;
    }

    static enum TransportHolder {
        INSTANCE;
        private final ReactorConfiguration transport;

        TransportHolder() {
            record nioConfiguration(String name, boolean isAvailable, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                                    Class<? extends ServerSocketChannel> serverChannelClass, Class<? extends SocketChannel> clientChannelClass)
                implements ReactorConfiguration {}

            transport = ServiceLoader.load(ReactorConfiguration.class)
                                     .stream()
                                     .map(ServiceLoader.Provider::get)
                                     .filter(ReactorConfiguration::isAvailable)
                                     .findFirst().orElseGet(() -> new nioConfiguration("NIO",
                                                                                       true,
                                                                                       new NioEventLoopGroup(1),
                                                                                       new NioEventLoopGroup(),
                                                                                       NioServerSocketChannel.class,
                                                                                       NioSocketChannel.class));

        }
    }
}
