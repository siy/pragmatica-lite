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

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import com.github.pgasync.async.IntermediatePromise;

/**
 * Builder for creating {@link Connectible} instances.
 *
 * @author Antti Laisi
 * @author Marat Gainullin
 */
public class NettyConnectibleBuilder extends ConnectibleBuilder {
    private IntermediatePromise<ProtocolStream> obtainStream() {
        return IntermediatePromise.successful(new NettyPgProtocolStream(
            //TODO: use resolver
            new InetSocketAddress(properties.hostname(), properties.port()),
            properties.useSsl(),
            Charset.forName(properties.encoding())
        ));
    }

    public Connectible pool() {
        return new PgConnectionPool(properties, this::obtainStream);
    }

    public Connectible plain() {
        return new PgDatabase(properties, this::obtainStream);
    }

}