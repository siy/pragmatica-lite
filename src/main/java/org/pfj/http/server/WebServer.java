package org.pfj.http.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pfj.lang.Cause;
import org.pfj.lang.Causes;
import org.pfj.lang.Promise;
import org.pfj.lang.Result;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;

public class WebServer {
	static {
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
	}
	private static final Logger log = LogManager.getLogger();

	private static final int DEFAULT_PORT = 8000;

	private final EndpointTable endpointTable;
	private final int port;

	private WebServer(int port, EndpointTable endpointTable) {
		this.port = port;
		this.endpointTable = endpointTable;
	}

	public static WebServer create(int port, RouteSource... routes) {
		return new WebServer(port, EndpointTable.with(routes));
	}

	public static WebServer create(RouteSource... routes) {
		return new WebServer(DEFAULT_PORT, EndpointTable.with(routes));
	}

	public Promise<Void> start() throws InterruptedException {
		EventLoopGroup bossGroup;
		EventLoopGroup workerGroup;
		Class<? extends ServerChannel> serverChannelClass;

		if (Epoll.isAvailable()) {
			bossGroup = new EpollEventLoopGroup(1);
			workerGroup = new EpollEventLoopGroup();
			serverChannelClass = EpollServerSocketChannel.class;
			log.info("Using epoll native transport");
		} else if (KQueue.isAvailable()) {
			bossGroup = new KQueueEventLoopGroup(1);
			workerGroup = new KQueueEventLoopGroup();
			serverChannelClass = KQueueServerSocketChannel.class;
			log.info("Using kqueue native transport");
		} else {
			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();
			serverChannelClass = NioServerSocketChannel.class;
			log.info("Using NIO transport");
		}

		endpointTable.print();

		var promise = Promise.<Void>promise();

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
				.addListener(future -> decode(promise, future));
		} finally {
			promise.onResult(__ -> {
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			});
		}

		return promise;
	}

	private Promise<Void> decode(Promise<Void> promise, Future<? super Void> future) {
		return promise.resolve(future.isSuccess()
							   ? Result.success(null)
							   : Causes.fromThrowable(future.cause()).result());
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
				ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			}

			var context = RequestContext.from(ctx, request);

			endpointTable.findRoute(request.method(), request.uri())
				.map(route -> safeCall(context, route.handler())
					.onResult(result -> respond(context, route, result)))
				.whenEmpty(() -> context.sendFailure(WebError.NOT_FOUND));
		}

		private RequestContext respond(RequestContext context, Route<?> route, Result<?> result) {
			return result.fold(
				failure -> context.sendFailure(convertError(failure)),
				success -> context.sendSuccess(route.contentType(), success)
			);
		}

		private Promise<?> safeCall(RequestContext context, Handler<?> handler) {
			try {
				return handler.handle(context);
			} catch (Throwable t) {
				return Promise.failure(CompoundCause.fromThrowable(WebError.INTERNAL_SERVER_ERROR, t));
			}
		}

		private CompoundCause convertError(Cause failure) {
			if (failure instanceof CompoundCause compoundCause) {
				return compoundCause;
			}

			return CompoundCause.from(WebError.INTERNAL_SERVER_ERROR.status(), failure);
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
}
