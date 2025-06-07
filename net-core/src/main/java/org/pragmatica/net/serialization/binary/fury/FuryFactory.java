package org.pragmatica.net.serialization.binary.fury;

import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;
import org.apache.fury.config.Language;
import org.pragmatica.net.serialization.binary.ClassRegistrator;

import java.util.stream.Stream;

public sealed interface FuryFactory {

    static ThreadSafeFury fury(ClassRegistrator... registrators) {
        int coreCount = Runtime.getRuntime().availableProcessors();
        var fury = Fury.builder()
                       .withLanguage(Language.JAVA)
                       .buildThreadSafeFuryPool(coreCount * 2, coreCount * 4);

        Stream.of(registrators)
              .forEach(registrator -> registrator.registerClasses(fury::register));

        return fury;
    }

    @SuppressWarnings("unused")
    record unused() implements FuryFactory {}
}
