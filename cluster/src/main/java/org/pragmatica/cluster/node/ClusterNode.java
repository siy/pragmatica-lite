package org.pragmatica.cluster.node;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.List;

public interface ClusterNode<C extends Command> {
    NodeId self();

    Promise<Unit> start();

    Promise<Unit> stop();

    <R> Promise<List<R>> apply(List<C> commands);
}
