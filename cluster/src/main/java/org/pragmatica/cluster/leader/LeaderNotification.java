package org.pragmatica.cluster.leader;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.lang.Option;
import org.pragmatica.message.Message;

public sealed interface LeaderNotification extends Message.Local {
    record LeaderChange(Option<NodeId> leaderId, boolean localNodeIsLeader) implements LeaderNotification { }

    static LeaderChange leaderChange(Option<NodeId> leaderId, boolean localNodeIsLeader) {
        return new LeaderChange(leaderId, localNodeIsLeader);
    }
}
