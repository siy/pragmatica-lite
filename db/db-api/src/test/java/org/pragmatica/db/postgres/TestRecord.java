package org.pragmatica.db.postgres;

import org.pragmatica.annotation.Template;

@Template
public record TestRecord(int id, String value) {
    public static TestRecordTemplate template() {
        return TestRecordTemplate.INSTANCE;
    }
}
