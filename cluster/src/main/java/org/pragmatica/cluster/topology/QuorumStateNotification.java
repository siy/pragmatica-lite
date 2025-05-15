package org.pragmatica.cluster.topology;

import org.pragmatica.message.Message;

/// Quorum state notifications
public enum QuorumStateNotification implements Message.Local {
    ESTABLISHED,
    DISAPPEARED,
}
