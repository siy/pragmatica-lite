/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.http.server;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.function.BiConsumer;

/**
 * HTTP server abstraction.
 */
public interface HttpServer {
    /**
     * Server port.
     */
    int port();

    /**
     * Stop the server.
     */
    Promise<Unit> stop();

    /**
     * Create and start an HTTP server.
     *
     * @param config  server configuration
     * @param handler request handler
     * @return promise of the running server
     */
    static Promise<HttpServer> httpServer(HttpServerConfig config, BiConsumer<RequestContext, ResponseWriter> handler) {
        return NettyHttpServer.create(config, handler);
    }
}
