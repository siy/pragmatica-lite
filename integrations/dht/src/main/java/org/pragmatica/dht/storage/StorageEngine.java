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

package org.pragmatica.dht.storage;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/// Storage engine interface for DHT data storage.
/// Implementations may use in-memory storage, off-heap memory, or persistent storage.
public interface StorageEngine {
    /// Get a value by key.
    ///
    /// @param key the key to look up
    /// @return the value if present, or empty option
    Promise<Option<byte[]>> get(byte[] key);

    /// Store a value.
    ///
    /// @param key   the key
    /// @param value the value to store
    /// @return promise that completes when value is stored
    Promise<Unit> put(byte[] key, byte[] value);

    /// Remove a value.
    ///
    /// @param key the key to remove
    /// @return true if value was present and removed, false if not found
    Promise<Boolean> remove(byte[] key);

    /// Check if a key exists.
    ///
    /// @param key the key to check
    /// @return true if key exists
    Promise<Boolean> exists(byte[] key);

    /// Get approximate number of entries.
    long size();

    /// Clear all entries.
    Promise<Unit> clear();

    /// Shutdown the storage engine and release resources.
    Promise<Unit> shutdown();
}
