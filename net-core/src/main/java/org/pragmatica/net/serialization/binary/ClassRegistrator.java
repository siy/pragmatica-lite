package org.pragmatica.net.serialization.binary;

import java.util.function.Consumer;

public interface ClassRegistrator {
    void registerClasses(Consumer<Class<?>> consumer);
}
