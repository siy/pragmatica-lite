package org.pragmatica.processor.record;

import org.pragmatica.annotation.Template;
import org.pragmatica.lang.Option;

@Template
public record NameAge(String firstName, String lastName, Option<String> middleName, int age) {
    public NameAge {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }
}
