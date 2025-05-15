package org.pragmatica.serialization.binary;

import java.util.function.Consumer;

public interface ClassRegistrator {
    void registerClasses(Consumer<Class<?>> consumer);
}
