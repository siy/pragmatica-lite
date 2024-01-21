package org.pragmatica.http;

public interface ContentType {
    String headerText();
    ContentCategory category();

    static ContentType custom(String headerText, ContentCategory category) {
        record contentType(String headerText, ContentCategory category) implements ContentType {}

        return new contentType(headerText, category);
    }
}
