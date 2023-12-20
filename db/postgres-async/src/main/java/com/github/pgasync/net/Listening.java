package com.github.pgasync.net;

import com.github.pgasync.async.IntermediatePromise;
import org.pragmatica.lang.Unit;

@FunctionalInterface
public interface Listening {

    IntermediatePromise<Unit> unlisten();
}
