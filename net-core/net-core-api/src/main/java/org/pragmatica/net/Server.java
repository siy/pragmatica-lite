package org.pragmatica.net;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.function.Supplier;

/**
 * Abstract server interface for managing server lifecycle and client connections.
 */
public interface Server {
    /// Get the server name
    String name();

    /// Connect to a peer server as a client
    Promise<Object> connectTo(NodeAddress peerLocation);

    /// Shutdown the server with an intermediate operation
    Promise<Unit> stop(Supplier<Promise<Unit>> intermediateOperation);

    /// Shutdown the server without intermediate operations
    default Promise<Unit> stop() {
        return stop(() -> Promise.success(Unit.unit()));
    }
}
