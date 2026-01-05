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

package org.pragmatica.http.websocket;

import java.util.function.Supplier;

/// WebSocket endpoint configuration.
///
/// @param path    the WebSocket path (e.g., "/ws/events")
/// @param handler supplier for WebSocket handler (called for each new connection)
public record WebSocketEndpoint(String path, Supplier<WebSocketHandler> handler) {
    public static WebSocketEndpoint webSocketEndpoint(String path, Supplier<WebSocketHandler> handler) {
        return new WebSocketEndpoint(path, handler);
    }

    public static WebSocketEndpoint webSocketEndpoint(String path, WebSocketHandler handler) {
        return new WebSocketEndpoint(path, () -> handler);
    }
}
