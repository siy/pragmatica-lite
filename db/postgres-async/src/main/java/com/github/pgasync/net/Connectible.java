package com.github.pgasync.net;

import org.pragmatica.lang.io.AsyncCloseable;

import java.util.concurrent.CompletableFuture;

/**
 * General container of connections.
 *
 * @author Marat Gainullin
 */
public interface Connectible extends QueryExecutor, AsyncCloseable {
    CompletableFuture<Connection> connection();

//    CompletableFuture<Void> close();
}
