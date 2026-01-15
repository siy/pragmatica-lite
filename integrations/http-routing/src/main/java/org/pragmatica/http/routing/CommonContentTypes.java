package org.pragmatica.http.routing;
public enum CommonContentTypes implements ContentType {
    TEXT_PLAIN("text/plain; charset=UTF-8", ContentCategory.PLAIN_TEXT),
    APPLICATION_JSON("application/json; charset=UTF-8", ContentCategory.JSON),
    APPLICATION_PROBLEM_JSON("application/problem+json; charset=UTF-8", ContentCategory.JSON),
    TEXT_HTML("text/html; charset=UTF-8", ContentCategory.HTML),
    APPLICATION_OCTET_STREAM("application/octet-stream", ContentCategory.BINARY);
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
