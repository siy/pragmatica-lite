package com.github.pgasync.net;

import com.github.pgasync.async.IntermediateFuture;

@FunctionalInterface
public interface Listening {

    IntermediateFuture<Void> unlisten();
}
