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

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/// In-memory storage engine backed by ConcurrentHashMap.
/// Thread-safe and suitable for development and testing.
/// Data is not persisted across restarts.
public final class MemoryStorageEngine implements StorageEngine {
    private final ConcurrentHashMap<ByteArrayKey, byte[]> data = new ConcurrentHashMap<>();

    private MemoryStorageEngine() {}

    public static MemoryStorageEngine memoryStorageEngine() {
        return new MemoryStorageEngine();
    }

    @Override
    public Promise<Option<byte[]>> get(byte[] key) {
        byte[] value = data.get(new ByteArrayKey(key));
        return Promise.success(value != null
                               ? Option.some(value.clone())
                               : Option.none());
    }

    @Override
    public Promise<Unit> put(byte[] key, byte[] value) {
        data.put(new ByteArrayKey(key), value.clone());
        return Promise.success(Unit.unit());
    }

    @Override
    public Promise<Boolean> remove(byte[] key) {
        byte[] removed = data.remove(new ByteArrayKey(key));
        return Promise.success(removed != null);
    }

    @Override
    public Promise<Boolean> exists(byte[] key) {
        return Promise.success(data.containsKey(new ByteArrayKey(key)));
    }

    @Override
    public long size() {
        return data.size();
    }

    @Override
    public Promise<Unit> clear() {
        data.clear();
        return Promise.success(Unit.unit());
    }

    @Override
    public Promise<Unit> shutdown() {
        data.clear();
        return Promise.success(Unit.unit());
    }

    /// Wrapper for byte[] to use as HashMap key with proper equals/hashCode.
    /// Clones input array to prevent external mutation from corrupting keys.
    private record ByteArrayKey(byte[] data) {
        ByteArrayKey(byte[] data) {
            this.data = data.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (! (o instanceof ByteArrayKey that)) return false;
            return Arrays.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
