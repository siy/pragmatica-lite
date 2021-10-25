package org.pfj.http.server.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public enum WebError implements CompoundCause {
    BAD_REQUEST(HttpResponseStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpResponseStatus.UNAUTHORIZED),
    PAYMENT_REQUIRED(HttpResponseStatus.PAYMENT_REQUIRED),
    FORBIDDEN(HttpResponseStatus.FORBIDDEN),
    NOT_FOUND(HttpResponseStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(HttpResponseStatus.METHOD_NOT_ALLOWED),
    NOT_ACCEPTABLE(HttpResponseStatus.NOT_ACCEPTABLE),
    REQUEST_TIMEOUT(HttpResponseStatus.REQUEST_TIMEOUT),
    CONFLICT(HttpResponseStatus.CONFLICT),
    GONE(HttpResponseStatus.GONE),
    LENGTH_REQUIRED(HttpResponseStatus.LENGTH_REQUIRED),
    PRECONDITION_FAILED(HttpResponseStatus.PRECONDITION_FAILED),
    REQUEST_ENTITY_TOO_LARGE(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE),
    REQUEST_URI_TOO_LONG(HttpResponseStatus.REQUEST_URI_TOO_LONG),
    UNSUPPORTED_MEDIA_TYPE(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE),
    REQUESTED_RANGE_NOT_SATISFIABLE(HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE),
    EXPECTATION_FAILED(HttpResponseStatus.EXPECTATION_FAILED),
    MISDIRECTED_REQUEST(HttpResponseStatus.MISDIRECTED_REQUEST),
    UNPROCESSABLE_ENTITY(HttpResponseStatus.UNPROCESSABLE_ENTITY),
    LOCKED(HttpResponseStatus.LOCKED),
    FAILED_DEPENDENCY(HttpResponseStatus.FAILED_DEPENDENCY),
    UNORDERED_COLLECTION(HttpResponseStatus.UNORDERED_COLLECTION),
    UPGRADE_REQUIRED(HttpResponseStatus.UPGRADE_REQUIRED),
    PRECONDITION_REQUIRED(HttpResponseStatus.PRECONDITION_REQUIRED),
    TOO_MANY_REQUESTS(HttpResponseStatus.TOO_MANY_REQUESTS),
    REQUEST_HEADER_FIELDS_TOO_LARGE(HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE),
    INTERNAL_SERVER_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR),
    NOT_IMPLEMENTED(HttpResponseStatus.NOT_IMPLEMENTED),
    BAD_GATEWAY(HttpResponseStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE(HttpResponseStatus.SERVICE_UNAVAILABLE),
    GATEWAY_TIMEOUT(HttpResponseStatus.GATEWAY_TIMEOUT),
    HTTP_VERSION_NOT_SUPPORTED(HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED),
    VARIANT_ALSO_NEGOTIATES(HttpResponseStatus.VARIANT_ALSO_NEGOTIATES),
    INSUFFICIENT_STORAGE(HttpResponseStatus.INSUFFICIENT_STORAGE),
    NOT_EXTENDED(HttpResponseStatus.NOT_EXTENDED),
    NETWORK_AUTHENTICATION_REQUIRED(HttpResponseStatus.NETWORK_AUTHENTICATION_REQUIRED);

    private final HttpResponseStatus status;

    WebError(HttpResponseStatus responseStatus) {
        this.status = responseStatus;
    }

    @Override
    public String message() {
        return status.reasonPhrase();
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }
}
