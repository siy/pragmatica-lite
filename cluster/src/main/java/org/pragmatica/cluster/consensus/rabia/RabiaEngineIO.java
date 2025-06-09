package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.state.Command;
import org.pragmatica.message.Message;

import java.util.List;

public interface RabiaEngineIO extends Message.Local {
    record SubmitCommands<C extends Command>(List<C> commands) implements RabiaEngineIO {}
}
