package org.pragmatica.cluster.consensus.rabia;

/// Represents the state values defined in the Rabia protocol (v0, v1, vquestion).
public enum StateValue {
    /// Negative vote (no agreement).
    V0,
    
    /// Positive vote (agreement).
    V1,
    
    /// Uncertain/question (needs coin flip).
    VQUESTION;
}
