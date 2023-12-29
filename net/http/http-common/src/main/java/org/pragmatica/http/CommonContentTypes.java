package org.pragmatica.http;

public enum CommonContentTypes implements ContentType {
    TEXT_PLAIN("text/plain; charset=UTF-8", ContentCategory.PLAIN_TEXT),
    APPLICATION_JSON("application/json; charset=UTF-8", ContentCategory.JSON),

    ;
    private final String headerText;
    private final ContentCategory category;

    CommonContentTypes(String headerText, ContentCategory category) {
        this.headerText = headerText;
        this.category = category;
    }

    @Override
    public String headerText() {
        return headerText;
    }

    @Override
    public ContentCategory category() {
        return category;
    }
}
