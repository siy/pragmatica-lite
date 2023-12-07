package org.pragmatica.http.protocol;

@SuppressWarnings("unused")
public enum CommonHeaders implements HttpHeaderName {
    ACCEPT("accept"),
    ACCEPT_CHARSET("accept-charset"),
    ACCEPT_ENCODING("accept-encoding"),
    ACCEPT_LANGUAGE("accept-language"),
    ACCEPT_RANGES("accept-ranges"),
    ACCEPT_PATCH("accept-patch"),
    ACCESS_CONTROL_ALLOW_CREDENTIALS("access-control-allow-credentials"),
    ACCESS_CONTROL_ALLOW_HEADERS("access-control-allow-headers"),
    ACCESS_CONTROL_ALLOW_METHODS("access-control-allow-methods"),
    ACCESS_CONTROL_ALLOW_ORIGIN("access-control-allow-origin"),
    ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK("access-control-allow-private-network"),
    ACCESS_CONTROL_EXPOSE_HEADERS("access-control-expose-headers"),
    ACCESS_CONTROL_MAX_AGE("access-control-max-age"),
    ACCESS_CONTROL_REQUEST_HEADERS("access-control-request-headers"),
    ACCESS_CONTROL_REQUEST_METHOD("access-control-request-method"),
    ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK("access-control-request-private-network"),
    AGE("age"),
    ALLOW("allow"),
    AUTHORIZATION("authorization"),
    CACHE_CONTROL("cache-control"),
    CONNECTION("connection"),
    CONTENT_BASE("content-base"),
    CONTENT_ENCODING("content-encoding"),
    CONTENT_LANGUAGE("content-language"),
    CONTENT_LENGTH("content-length"),
    CONTENT_LOCATION("content-location"),
    CONTENT_TRANSFER_ENCODING("content-transfer-encoding"),
    CONTENT_DISPOSITION("content-disposition"),
    CONTENT_MD5("content-md5"),
    CONTENT_RANGE("content-range"),
    CONTENT_SECURITY_POLICY("content-security-policy"),
    CONTENT_TYPE("content-type"),
    COOKIE("cookie"),
    DATE("date"),
    EXPECT("expect"),
    EXPIRES("expires"),
    FROM("from"),
    HOST("host"),
    IF_MATCH("if-match"),
    IF_MODIFIED_SINCE("if-modified-since"),
    IF_NONE_MATCH("if-none-match"),
    IF_RANGE("if-range"),
    IF_UNMODIFIED_SINCE("if-unmodified-since"),
    KEEP_ALIVE("keep-alive"),
    LAST_MODIFIED("last-modified"),
    LOCATION("location"),
    MAX_FORWARDS("max-forwards"),
    ORIGIN("origin"),
    PROXY_AUTHENTICATE("proxy-authenticate"),
    PROXY_AUTHORIZATION("proxy-authorization"),
    RANGE("range"),
    REFERER("referer"),
    RETRY_AFTER("retry-after"),
    SERVER("server"),
    SET_COOKIE("set-cookie"),
    SET_COOKIE2("set-cookie2"),
    TRANSFER_ENCODING("transfer-encoding"),
    USER_AGENT("user-agent"),
    WWW_AUTHENTICATE("www-authenticate"),
    REQUEST_ID("request-id"),
    REQUEST_ID_CHAIN("request-id-chain");
    private final String nameString;

    CommonHeaders(String nameString) {
        this.nameString = nameString;
    }

    @Override
    public String asString() {
        return nameString;
    }
}
