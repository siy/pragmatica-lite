package com.github.pgasync.net;

import com.github.pgasync.async.IntermediatePromise;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor {
    IntermediatePromise<Connection> getConnection();

    IntermediatePromise<Void> close();
}
