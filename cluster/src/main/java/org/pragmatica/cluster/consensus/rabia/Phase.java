package org.pragmatica.cluster.consensus.rabia;

/// Represents a protocol phase as defined in the Rabia formal specification.
public record Phase(long value) implements Comparable<Phase> {
    public static final Phase ZERO = new Phase(0);

    /// Creates the successor phase to this phase.
    ///
    /// @return The next phase
    public Phase successor() {
        return new Phase(value + 1);
    }

    @Override
    public int compareTo(Phase other) {
        return Long.compare(this.value, other.value);
    }
}
