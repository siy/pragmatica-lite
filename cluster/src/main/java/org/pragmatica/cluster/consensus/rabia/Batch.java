package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.state.Command;

import java.util.List;

/// Represents a proposal value in the Rabia protocol.
public record Batch<C extends Command>(BatchId id, CorrelationId correlationId, long timestamp, List<C> commands) implements Comparable<Batch<C>> {
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

        return correlationId.id().compareTo(o.correlationId.id());
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

    public boolean isNotEmpty() {
        return !commands.isEmpty();
    }
}
