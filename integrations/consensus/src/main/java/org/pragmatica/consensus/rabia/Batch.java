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

import java.util.List;

/**
 * Represents a proposal value (batch of commands) in the Rabia protocol.
 */
public record Batch<C extends Command>(BatchId id,
                                       CorrelationId correlationId,
                                       long timestamp,
                                       List<C> commands) implements Comparable<Batch<C>> {
    @Override
    public int compareTo(Batch<C> o) {
        var timestampCompare = Long.compare(timestamp, o.timestamp);
        if (timestampCompare != 0) {
            return timestampCompare;
        }
        var idCompare = Integer.compare(id.hashCode(), o.id.hashCode());
        if (idCompare != 0) {
            return idCompare;
        }
        return correlationId.id()
                            .compareTo(o.correlationId.id());
    }

    public static <C extends Command> Batch<C> batch(List<C> commands) {
        return new Batch<>(BatchId.randomBatchId(), CorrelationId.randomCorrelationId(), System.nanoTime(), commands);
    }

    public static <C extends Command> Batch<C> emptyBatch() {
        return new Batch<>(BatchId.emptyBatchId(), CorrelationId.emptyCorrelationId(), System.nanoTime(), List.of());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof Batch< ? > batch)) return false;
        return id.equals(batch.id);
    }

    public boolean isNotEmpty() {
        return ! commands.isEmpty();
    }
}
