package com.github.pgasync.net;

import com.github.pgasync.async.ThrowingPromise;
import org.pragmatica.lang.Unit;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor {
    ThrowingPromise<Connection> getConnection();

    ThrowingPromise<Unit> close();
}
