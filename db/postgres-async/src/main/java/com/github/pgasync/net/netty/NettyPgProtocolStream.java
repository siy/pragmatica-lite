/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pgasync.net.netty;

import com.github.pgasync.PgProtocolStream;
import com.github.pgasync.async.ThrowableCause;
import com.github.pgasync.async.ThrowingPromise;
import com.github.pgasync.message.Message;
import com.github.pgasync.message.backend.SslHandshake;
import com.github.pgasync.message.frontend.SSLRequest;
import com.github.pgasync.message.frontend.StartupMessage;
import com.github.pgasync.message.frontend.Terminate;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.pragmatica.lang.Unit;
import org.pragmatica.net.transport.api.TransportConfiguration;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Netty messages stream to Postgres backend.
 *
 * @author Antti Laisi
 */
public class NettyPgProtocolStream extends PgProtocolStream {
    protected final boolean useSsl;
    private final SocketAddress address;
    private final Bootstrap channelPipeline;
    private StartupMessage startupWith;
    private ChannelHandlerContext ctx;

    private final GenericFutureListener<Future<? super Object>> outboundErrorListener = written -> {
        if (!written.isSuccess()) {
            gotError(ThrowableCause.asCause(written.cause()));
        }
    };

    public NettyPgProtocolStream(SocketAddress address, boolean useSsl, Charset encoding) {
        super(encoding);
        this.address = address;
        this.useSsl = useSsl; // TODO: refactor into SSLConfig with trust parameters
        this.channelPipeline = TransportConfiguration.transportConfiguration()
                                                     .clientBootstrap()
                                                     .handler(newProtocolInitializer());
    }

    @Override
    public ThrowingPromise<Message> connect(StartupMessage startup) {
        startupWith = startup;
        return offerRoundTrip(() -> channelPipeline.connect(address).addListener(outboundErrorListener), false)
            .flatMap(this::send)
            .flatMap(message -> {
                if (message == SslHandshake.INSTANCE) {
                    return send(startup);
                } else {
                    return ThrowingPromise.successful(message);
                }
            });
    }

    @Override
    public boolean isConnected() {
        return ctx.channel().isOpen();
    }

    @Override
    public ThrowingPromise<Unit> close() {
        var uponClose = ThrowingPromise.<Unit>create();

        ctx.writeAndFlush(Terminate.INSTANCE)
           .addListener(written -> {
               if (written.isSuccess()) {
                   ctx.close()
                      .addListener(closed -> {
                          if (closed.isSuccess()) {
                              uponClose.succeedAsync(Unit::aUnit);
                          } else {
                              uponClose.failAsync(() -> ThrowableCause.asCause(closed.cause()));
                          }
                      });
               } else {
                   uponClose.failAsync(() -> ThrowableCause.asCause(written.cause()));
               }
           });
        return uponClose;
    }

    @Override
    protected void write(Message... messages) {
        for (Message message : messages) {
            ctx.write(message)
               .addListener(outboundErrorListener);
        }
        ctx.flush();
    }

    private ChannelInitializer<Channel> newProtocolInitializer() {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) {
                var pipeline = channel.pipeline();

                if (useSsl) {
                    pipeline.addLast(newSslInitiator());
                }
                pipeline
                    .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 1, 4, -4, 0, true))
                    .addLast(new NettyMessageDecoder(encoding))
                    .addLast(new NettyMessageEncoder(encoding))
                    .addLast(newProtocolHandler());
            }
        };
    }

    private ChannelHandler newSslInitiator() {
        return new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                if (in.readableBytes() >= 1) {
                    if ('S' == in.readByte()) { // SSL supported response
                        //TODO: take SSL configuration from config
                        ctx.pipeline().remove(this);
                        ctx.pipeline().addFirst(
                            SslContextBuilder
                                .forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build()
                                .newHandler(ctx.alloc()));
                    } else {
                        ctx.fireExceptionCaught(new IllegalStateException("SSL required but not supported by Postgres"));
                    }
                }
            }
        };
    }

    private ChannelHandler newProtocolHandler() {
        return new ChannelInboundHandlerAdapter() {

            @Override
            public void channelActive(ChannelHandlerContext context) {
                NettyPgProtocolStream.this.ctx = context;
                if (useSsl) {
                    gotMessage(SSLRequest.INSTANCE);
                } else {
                    gotMessage(startupWith);
                }
            }

            @Override
            public void userEventTriggered(ChannelHandlerContext context, Object evt) {
                if (evt instanceof SslHandshakeCompletionEvent handshake && handshake.isSuccess()) {
                    gotMessage(SslHandshake.INSTANCE);
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext context, Object message) {
                if (message instanceof Message msg) {
                    gotMessage(msg);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext context) {
                gotError(ThrowableCause.asCause(new IOException("Channel state changed to inactive")));
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                gotError(ThrowableCause.asCause(cause));
            }
        };
    }
}
