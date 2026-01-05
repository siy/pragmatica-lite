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

    public Partition {
        if (value < 0 || value >= MAX_PARTITIONS) {
            throw new IllegalArgumentException(INVALID_PARTITION.message());
        }
    }

    public static Result<Partition> partition(int value) {
        if (value < 0 || value >= MAX_PARTITIONS) {
            return INVALID_PARTITION.result();
        }
        return Result.success(new Partition(value));
    }

    public static Partition partitionUnsafe(int value) {
        return new Partition(value);
    }

    /// Get the next partition in the ring (wraps around).
    public Partition next() {
        return new Partition((value + 1) % MAX_PARTITIONS);
    }

    /// Get the previous partition in the ring (wraps around).
    public Partition previous() {
        return new Partition((value - 1 + MAX_PARTITIONS) % MAX_PARTITIONS);
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
