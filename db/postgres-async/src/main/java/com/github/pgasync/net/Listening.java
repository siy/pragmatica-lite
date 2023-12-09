package com.github.pgasync.net;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Listening {

    CompletableFuture<Void> unlisten();
}
