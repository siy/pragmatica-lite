package org.pragmatica.net.serialization.binary.fury;

import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;
import org.pragmatica.serialization.binary.ClassRegistrator;

public sealed interface FuryFactory {

    static ThreadSafeFury fury(ClassRegistrator registrator) {
        int coreCount = Runtime.getRuntime().availableProcessors();
        var fury = Fury.builder()
                       .withLanguage(Language.JAVA)
                       .buildThreadSafeFuryPool(coreCount * 2, coreCount * 4);

        registrator.registerClasses(fury::register);

        return fury;
    }

    @SuppressWarnings("unused")
    record unused() implements FuryFactory {}
}
