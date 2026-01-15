package org.pragmatica.http.routing;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;

/**
 * RFC 7807 Problem Details for HTTP APIs.
 * <p>
 * Provides a standardized format for returning error information in HTTP responses.
 * See <a href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807</a>.
 * <p>
 * Extension: {@code requestId} is mandatory for request tracing.
 *
 * @param type      URI reference identifying the problem type (default: "about:blank")
 * @param title     Short human-readable summary of the problem type
 * @param status    HTTP status code
 * @param detail    Human-readable explanation specific to this occurrence (MAY be present per RFC 7807)
 * @param instance  URI reference identifying the specific occurrence (MAY be present per RFC 7807)
 * @param requestId Unique identifier for the request (mandatory extension)
 */
public record ProblemDetail(String type,
                            String title,
                            int status,
                            Option<String> detail,
                            Option<String> instance,
                            String requestId) {
    private static final String DEFAULT_TYPE = "about:blank";

    /**
     * Create a ProblemDetail from an HttpError.
     *
     * @param error     The HTTP error
     * @param instance  Request path/URI
     * @param requestId Request identifier
     * @return ProblemDetail instance
     */
    public static ProblemDetail fromHttpError(HttpError error, String instance, String requestId) {
        return new ProblemDetail(DEFAULT_TYPE,
                                 error.status()
                                      .message(),
                                 error.status()
                                      .code(),
                                 Option.option(extractDetail(error)),
                                 Option.option(instance),
                                 requestId);
    }

    /**
     * Create a ProblemDetail from a generic Cause (defaults to 500 Internal Server Error).
     *
     * @param cause     The error cause
     * @param instance  Request path/URI
     * @param requestId Request identifier
     * @return ProblemDetail instance
     */
    public static ProblemDetail fromCause(Cause cause, String instance, String requestId) {
        return new ProblemDetail(DEFAULT_TYPE,
                                 HttpStatus.INTERNAL_SERVER_ERROR.message(),
                                 HttpStatus.INTERNAL_SERVER_ERROR.code(),
                                 Option.option(cause.message()),
                                 Option.option(instance),
                                 requestId);
    }

    /**
     * Create a ProblemDetail with custom type URI.
     *
     * @param type      Problem type URI
     * @param status    HTTP status
     * @param detail    Error detail message
     * @param instance  Request path/URI
     * @param requestId Request identifier
     * @return ProblemDetail instance
     */
    public static ProblemDetail problemDetail(String type,
                                              HttpStatus status,
                                              String detail,
                                              String instance,
                                              String requestId) {
        return new ProblemDetail(type,
                                 status.message(),
                                 status.code(),
                                 Option.option(detail),
                                 Option.option(instance),
                                 requestId);
    }

    /**
     * Create a ProblemDetail with default type.
     *
     * @param status    HTTP status
     * @param detail    Error detail message
     * @param instance  Request path/URI
     * @param requestId Request identifier
     * @return ProblemDetail instance
     */
    public static ProblemDetail problemDetail(HttpStatus status,
                                              String detail,
                                              String instance,
                                              String requestId) {
        return new ProblemDetail(DEFAULT_TYPE,
                                 status.message(),
                                 status.code(),
                                 Option.option(detail),
                                 Option.option(instance),
                                 requestId);
    }

    private static String extractDetail(HttpError error) {
        // Get the origin cause message, not the full chain
        return error.source()
                    .map(Cause::message)
                    .or(error.status()
                             .message());
    }
}
