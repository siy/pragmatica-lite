package org.pragmatica.http.routing;
public interface ContentType {
    String headerText();

    ContentCategory category();

    static ContentType contentType(String headerText, ContentCategory category) {
        record contentType(String headerText, ContentCategory category) implements ContentType {}
        return new contentType(headerText, category);
    }
}
