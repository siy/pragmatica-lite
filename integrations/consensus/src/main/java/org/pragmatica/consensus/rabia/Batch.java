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

package org.pragmatica.consensus.rabia;

import org.pragmatica.consensus.Command;

import java.util.ArrayList;
import java.util.List;

/// Represents a proposal value (batch of commands) in the Rabia protocol.
/// BatchId is content-based (hash of commands) to enable consolidation of identical batches.
public record Batch<C extends Command>(BatchId id,
                                       List<CorrelationId> correlationIds,
                                       long timestamp,
                                       List<C> commands) implements Comparable<Batch<C>> {
    public Batch {
        commands = List.copyOf(commands);
        correlationIds = List.copyOf(correlationIds);
    }

    @Override
    public int compareTo(Batch<C> o) {
        var timestampCompare = Long.compare(timestamp, o.timestamp);
        if (timestampCompare != 0) {
            return timestampCompare;
        }
        return id.id()
                 .compareTo(o.id.id());
    }

    public static <C extends Command> Batch<C> batch(List<C> commands) {
        var contentHash = computeContentHash(commands);
        var batchId = new BatchId("batch-" + contentHash);
        return new Batch<>(batchId,
                           List.of(CorrelationId.randomCorrelationId()),
                           System.nanoTime(),
                           commands);
    }

    private static <C extends Command> String computeContentHash(List<C> commands) {
        return Integer.toHexString(commands.hashCode());
    }

    public static <C extends Command> Batch<C> emptyBatch() {
        return new Batch<>(BatchId.emptyBatchId(),
                           List.of(CorrelationId.emptyCorrelationId()),
                           System.nanoTime(),
                           List.of());
    }

    /// Merges this batch with another batch that has the same content.
    /// Combines correlationIds and uses the earliest timestamp.
    public Batch<C> mergeWith(Batch<C> other) {
        var merged = new ArrayList<>(correlationIds);
        merged.addAll(other.correlationIds);
        var earliestTimestamp = Math.min(timestamp, other.timestamp);
        return new Batch<>(id, List.copyOf(merged), earliestTimestamp, commands);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof Batch<?> batch)) return false;
        return id.equals(batch.id);
    }

    public boolean isNotEmpty() {
        return ! commands.isEmpty();
    }
}
