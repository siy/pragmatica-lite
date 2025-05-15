package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.utility.IdGenerator;

public record CorrelationId(String id) {
    public static CorrelationId correlationId(String id) {
        return new CorrelationId(id);
    }

    public static CorrelationId randomCorrelationId() {
        return correlationId(IdGenerator.generate("xref"));
    }

    public static CorrelationId emptyCorrelationId() {
        return correlationId("");
    }
}
