package org.pragmatica.processor.record;

import org.pragmatica.annotation.Template;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Tuple.Tuple2;

import java.time.LocalDate;
import java.util.List;

@Template
public record NameAge(String firstName, String lastName, Option<String> middleName, int age, List<Tuple2<String, LocalDate>> children) {
    public NameAge {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }
}
