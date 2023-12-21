package com.github.pgasync.net;

import com.github.pgasync.async.ThrowingPromise;
import org.pragmatica.lang.Unit;

@FunctionalInterface
public interface Listening {

    ThrowingPromise<Unit> unlisten();
}
