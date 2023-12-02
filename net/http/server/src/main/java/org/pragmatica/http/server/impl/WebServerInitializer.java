package org.pragmatica.http.server.impl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsHandler;
import org.pragmatica.http.server.config.WebServerConfiguration;
import org.pragmatica.http.server.routing.RequestRouter;

public class WebServerInitializer extends ChannelInitializer<SocketChannel> {
    private final WebServerConfiguration configuration;
    private final RequestRouter requestRouter;

    public WebServerInitializer(WebServerConfiguration configuration, RequestRouter requestRouter) {
        this.configuration = configuration;
        this.requestRouter = requestRouter;
    }

    @Override
    public void initChannel(SocketChannel channel) {
        var pipeline = configureSsl(channel)
            .addLast(new HttpResponseEncoder())
            .addLast(new HttpRequestDecoder())
            .addLast(new HttpObjectAggregator(configuration.maxContentLen()));

        configureCors(pipeline)
            .addLast(new WebServerHandler(configuration, requestRouter));
    }

    private ChannelPipeline configureSsl(SocketChannel channel) {
        var pipeline = channel.pipeline();

        configuration.sslContext()
                     .onPresent(sslContext -> pipeline.addLast(sslContext.newHandler(channel.alloc())));

        return pipeline;
    }

    private ChannelPipeline configureCors(ChannelPipeline pipeline) {
        configuration.corsConfig()
                     .onPresent(corsConfig -> pipeline.addLast(new CorsHandler(corsConfig)));

        return pipeline;
    }
}
