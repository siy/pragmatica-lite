package com.github.pgasync.net;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.io.AsyncCloseable;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor, AsyncCloseable {
    Promise<Connection> connection();
}
