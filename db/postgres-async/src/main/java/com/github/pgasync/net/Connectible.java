package com.github.pgasync.net;

import com.github.pgasync.async.IntermediateFuture;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor {
    IntermediateFuture<Connection> getConnection();

    IntermediateFuture<Void> close();
}
