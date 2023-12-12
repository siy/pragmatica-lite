package com.github.pgasync.net;

import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

@FunctionalInterface
public interface Listening {

    Promise<Unit> unlisten();
}
