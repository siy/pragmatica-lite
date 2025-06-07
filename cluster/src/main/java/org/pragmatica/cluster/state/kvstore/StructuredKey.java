package org.pragmatica.cluster.state.kvstore;

public interface StructuredKey {
    boolean matches(StructuredPattern pattern);
}
