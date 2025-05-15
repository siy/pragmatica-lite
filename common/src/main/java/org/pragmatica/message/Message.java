package org.pragmatica.message;

/// Marker interface for all messages circulating inside the system
public sealed interface Message {
    non-sealed interface Local extends Message {
    }

    non-sealed interface Wired extends Message {
    }
}
