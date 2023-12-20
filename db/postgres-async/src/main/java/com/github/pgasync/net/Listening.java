package com.github.pgasync.net;

import com.github.pgasync.async.IntermediatePromise;

@FunctionalInterface
public interface Listening {

    IntermediatePromise<Void> unlisten();
}
