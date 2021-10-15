package org.pfj.http.server;

import com.jsoniter.output.JsonStream;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.pfj.lang.Cause;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.pfj.http.server.ContentType.APPLICATION_JSON;
import static org.pfj.http.server.ContentType.TEXT_PLAIN;

public class WebServer {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final int DEFAULT_PORT = 8000;
    private static final String SERVER_NAME = "PFJ Netty Server";

    private final EndpointTable endpointTable = new EndpointTable();
    private final int port;

    private WebServer(int port) {
        this.port = port;
    }

    public static WebServer create() {
        return create(DEFAULT_PORT);
    }

    public static WebServer create(int port) {
        return new WebServer(port);
    }

    public <T> WebServer getText(final String path, final Handler<T> handler) {
        this.endpointTable.add(Route.getText(path, handler));
        return this;
    }

    public <T> WebServer getJson(final String path, final Handler<T> handler) {
        this.endpointTable.add(Route.getJson(path, handler));
        return this;
    }

    public <T> WebServer postText(final String path, final Handler<T> handler) {
        this.endpointTable.add(Route.postText(path, handler));
        return this;
    }

    public <T> WebServer postJson(final String path, final Handler<T> handler) {
        this.endpointTable.add(Route.postJson(path, handler));
        return this;
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerChannel> serverChannelClass;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            serverChannelClass = EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1);
            workerGroup = new KQueueEventLoopGroup();
            serverChannelClass = KQueueServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            serverChannelClass = NioServerSocketChannel.class;
        }

        try {
            new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 32 * 1024)

                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new WebServerInitializer())
                .bind(port)
                .sync()
                .channel()
                .closeFuture()
                .sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class WebServerInitializer extends ChannelInitializer<SocketChannel> {
        public static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline()
                .addLast("decoder", new HttpRequestDecoder())
                .addLast("aggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                .addLast("encoder", new HttpResponseEncoder())
                .addLast("handler", new WebServerHandler());
        }
    }

    private class WebServerHandler extends SimpleChannelInboundHandler<Object> {
        /**
         * Handles a new message.
         *
         * @param ctx The channel context.
         * @param msg The HTTP request message.
         */
        @Override
        public void channelRead0(final ChannelHandlerContext ctx, final Object msg) {
            if (!(msg instanceof final FullHttpRequest request)) {
                return;
            }

            if (HttpUtil.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            try {
                endpointTable
                    .findRoute(request.method(), request.uri())
                    .toResult(WebError.NOT_FOUND)
                    .map(route -> route.handler()
                        .handle(request)
                        .fold(
                            failure -> writeResponse(ctx, request, convertError(failure), TEXT_PLAIN, failure.message()),
                            success -> writeResponse(ctx, request, HttpResponseStatus.OK, route.contentType(), serializeResponse(route.contentType(), success))
                        )
                    );
            } catch (final RuntimeException ex) {
                var sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                writeResponse(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR, TEXT_PLAIN, sw.toString());
            }
        }

        private ByteBuf serializeResponse(ContentType contentType, Object success) {
            return switch (contentType) {
                case TEXT_PLAIN -> asByteBuf(success.toString());
                case APPLICATION_JSON -> asByteBuf(JsonStream.serialize(success));
            };
        }

        private HttpResponseStatus convertError(Cause failure) {
            if (failure instanceof WebError webError) {
                return webError.status();
            }

            return HttpResponseStatus.INTERNAL_SERVER_ERROR;
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
            ctx.close();
        }

        @Override
        public void channelReadComplete(final ChannelHandlerContext ctx) {
            ctx.flush();
        }
    }

    private static long writeResponse(
        ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, ContentType contentType, String content
    ) {
        return writeResponse(ctx, request, status, contentType, asByteBuf(content));
    }

    private static ByteBuf asByteBuf(String content) {
        return Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8));
    }

    private static long writeResponse(
        ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, ContentType contentType, ByteBuf entity
    ) {
        var keepAlive = HttpUtil.isKeepAlive(request);
        var response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, entity);

        response.headers()
            .set(HttpHeaderNames.SERVER, SERVER_NAME)
            .set(HttpHeaderNames.DATE, ZonedDateTime.now().format(DATETIME_FORMATTER))
            .set(HttpHeaderNames.CONTENT_TYPE, contentType.text())
            .set(HttpHeaderNames.CONTENT_LENGTH, Long.toString(entity.maxCapacity()));

        if (!keepAlive) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.writeAndFlush(response, ctx.voidPromise());
        }

        return entity.maxCapacity();
    }

    private static void send100Continue(final ChannelHandlerContext ctx) {
        ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }
}
