package org.pragmatica.utility;

import java.util.Locale;

public sealed interface IdGenerator {

    static String generate(String prefix) {
        return prefix + "_" + ULID.randomULID().encoded().toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unused")
    record unused() implements IdGenerator {}
}
