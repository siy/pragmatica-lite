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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;

/// A partition identifier in the DHT consistent hash ring.
/// Partitions are numbered 0 to MAX_PARTITIONS-1 (default 1024 partitions).
public record Partition(int value) implements Comparable<Partition> {
    public static final int MAX_PARTITIONS = 1024;

    private static final Cause INVALID_PARTITION = Causes.cause("Partition must be between 0 and " + (MAX_PARTITIONS - 1));

    // Pre-allocated array of all valid partitions for efficient access
    private static final Partition[] PARTITIONS = new Partition[MAX_PARTITIONS];

    static {
        for (int i = 0; i < MAX_PARTITIONS; i++) {
            PARTITIONS[i] = new Partition(i);
        }
    }

    public static Result<Partition> partition(int value) {
        if (value < 0 || value >= MAX_PARTITIONS) {
            return INVALID_PARTITION.result();
        }
        return Result.success(PARTITIONS[value]);
    }

    /// Internal access for known-valid values (0 to MAX_PARTITIONS-1).
    /// Used by next(), previous(), and internal iteration.
    static Partition at(int value) {
        return PARTITIONS[value];
    }

    /// Get the next partition in the ring (wraps around).
    public Partition next() {
        return at((value + 1) % MAX_PARTITIONS);
    }

    /// Get the previous partition in the ring (wraps around).
    public Partition previous() {
        return at((value - 1 + MAX_PARTITIONS) % MAX_PARTITIONS);
    }

    @Override
    public int compareTo(Partition other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return "P" + value;
    }
}
