package org.pragmatica.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.utils.Causes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

/**
 * Convenient wrapper for Netty server setup boilerplate. It also enables connecting to other servers as client,
 * reusing the same channel handlers.
 */
public interface Server {
    String name();

    Promise<Channel> connectTo(NodeAddress peerLocation);

    /**
     * Shutdown the server
     *
     * @param intermediateOperation Operation to run between server channel shutdown and event loop groups shutdown
     */
    Promise<Unit> stop(Supplier<Promise<Unit>> intermediateOperation);

    static Promise<Server> server(String name, int port, Supplier<List<ChannelHandler>> channelHandlers) {
        record server(String name, int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup,
                      Channel serverChannel,
                      Supplier<List<ChannelHandler>> channelHandlers) implements Server {
            private static final Logger log = LoggerFactory.getLogger(Server.class);

            @Override
            public Promise<Unit> stop(Supplier<Promise<Unit>> intermediate) {
                var stopPromise = Promise.<Unit>promise();
                log.trace("Stopping {}: closing server channel", name());

                serverChannel.close()
                             .addListener(_ ->
                                                  intermediate.get()
                                                              .onResult(_ -> shutdownGroups())
                                                              .onResult(stopPromise::resolve));

                return stopPromise;
            }

            private void shutdownGroups() {
                log.debug("Stopping {}: shutting down boss group", name());
                bossGroup.shutdownGracefully();

                log.debug("Stopping {}: shutting down worker group", name());
                workerGroup.shutdownGracefully();
                log.info("Server {} stopped", name());
            }

            @Override
            public Promise<Channel> connectTo(NodeAddress address) {
                var bootstrap = new Bootstrap()
                        .group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(server.createChildHandler(channelHandlers));

                var promise = Promise.<Channel>promise();
                bootstrap.connect(address.host(), address.port())
                         .addListener((ChannelFutureListener) future -> {
                             if (future.isSuccess()) {
                                 promise.succeed(future.channel());
                             } else {
                                 promise.fail(Causes.fromThrowable(future.cause()));
                             }
                         });
                return promise;
            }

            private static ChannelInitializer<SocketChannel> createChildHandler(Supplier<List<ChannelHandler>> channelHandlers) {
                return new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        var pipeline = ch.pipeline();

                        for (var handler : channelHandlers.get()) {
                            pipeline.addLast(handler);
                        }
                    }
                };
            }
        }

        var bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        var workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        var bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(server.createChildHandler(channelHandlers));

        var promise = Promise.<Server>promise();

        bootstrap.bind(port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                server.log.info("Server {} started on port {}", name, port);
                promise.succeed(new server(name, port, bossGroup, workerGroup, future.channel(), channelHandlers));
            } else {
                promise.fail(Causes.fromThrowable(future.cause()));
            }
        });

        return promise;
    }
}
