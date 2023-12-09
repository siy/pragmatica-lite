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

import com.github.pgasync.PgConnectionPool;
import com.github.pgasync.PgDatabase;
import com.github.pgasync.ProtocolStream;
import com.github.pgasync.net.Connectible;
import com.github.pgasync.net.ConnectibleBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Builder for creating {@link Connectible} instances.
 *
 * @author Antti Laisi
 * @author Marat Gainullin
 */
public class NettyConnectibleBuilder extends ConnectibleBuilder {

    // TODO: refactor when Netty will support more advanced threading model
    //new NioEventLoopGroup(0/*Netty defaults will be used*/, futuresExecutor),
    private static final EventLoopGroup group = new NioEventLoopGroup();

    private CompletableFuture<ProtocolStream> obtainStream(Executor futuresExecutor) {
        return CompletableFuture.completedFuture(new NettyPgProtocolStream(
                group,
                new InetSocketAddress(properties.getHostname(), properties.getPort()),
                properties.getUseSsl(),
                Charset.forName(properties.getEncoding()),
                futuresExecutor
        ));
    }

    public Connectible pool(Executor futuresExecutor) {
        return new PgConnectionPool(properties, () -> obtainStream(futuresExecutor), futuresExecutor);
    }

    public Connectible plain(Executor futuresExecutor) {
        return new PgDatabase(properties, () -> obtainStream(futuresExecutor), futuresExecutor);
    }

}