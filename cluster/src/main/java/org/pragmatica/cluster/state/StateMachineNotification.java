package org.pragmatica.cluster.state;

import org.pragmatica.message.Message;

/// State machine notifications root.
public interface StateMachineNotification extends Message.Local {
    <T extends Command> T cause();
}
