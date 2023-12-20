package com.github.pgasync.net;

import com.github.pgasync.async.IntermediatePromise;
import org.pragmatica.lang.Unit;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor {
    IntermediatePromise<Connection> getConnection();

    IntermediatePromise<Unit> close();
}
