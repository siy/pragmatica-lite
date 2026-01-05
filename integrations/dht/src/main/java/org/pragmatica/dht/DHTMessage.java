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

package org.pragmatica.dht;

import org.pragmatica.lang.Option;
import org.pragmatica.messaging.Message;

/// Messages for DHT operations between nodes.
public sealed interface DHTMessage extends Message.Wired {
    /// Request to get a value.
    record GetRequest(String requestId, byte[] key) implements DHTMessage {}

    /// Response to a get request.
    record GetResponse(String requestId, Option<byte[]> value) implements DHTMessage {}

    /// Request to put a value.
    record PutRequest(String requestId, byte[] key, byte[] value) implements DHTMessage {}

    /// Response to a put request.
    record PutResponse(String requestId, boolean success) implements DHTMessage {}

    /// Request to remove a value.
    record RemoveRequest(String requestId, byte[] key) implements DHTMessage {}

    /// Response to a remove request.
    record RemoveResponse(String requestId, boolean found) implements DHTMessage {}

    /// Request to check if key exists.
    record ExistsRequest(String requestId, byte[] key) implements DHTMessage {}

    /// Response to exists request.
    record ExistsResponse(String requestId, boolean exists) implements DHTMessage {}
}
