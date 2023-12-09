package com.github.pgasync.net;

import java.util.concurrent.CompletableFuture;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor {

    CompletableFuture<Connection> getConnection();

    CompletableFuture<Void> close();
}
