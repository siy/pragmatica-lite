package org.pragmatica.http.routing;

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Promise;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import static org.pragmatica.http.routing.ContentCategory.BINARY;
import static org.pragmatica.http.routing.ContentCategory.HTML;
import static org.pragmatica.http.routing.ContentCategory.JSON;
import static org.pragmatica.http.routing.ContentCategory.PLAIN_TEXT;
import static org.pragmatica.http.routing.ContentType.contentType;
import static org.pragmatica.http.routing.HttpMethod.GET;
import static org.pragmatica.http.routing.Route.route;

/**
 * Route source for serving static files from the classpath.
 * <p>
 * Supports common web content types and provides security against directory traversal attacks.
 * <p>
 * Example usage:
 * <pre>{@code
 * var routes = StaticFileRouteSource.staticFiles("/static", "/web");
 * // Serves /static/app.js from classpath:/web/app.js
 * }</pre>
 */
public interface StaticFileRouteSource extends RouteSource {
    Map<String, ContentType> CONTENT_TYPES = Map.ofEntries(Map.entry(".html",
                                                                     contentType("text/html; charset=UTF-8", HTML)),
                                                           Map.entry(".htm",
                                                                     contentType("text/html; charset=UTF-8", HTML)),
                                                           Map.entry(".css",
                                                                     contentType("text/css; charset=UTF-8", PLAIN_TEXT)),
                                                           Map.entry(".js",
                                                                     contentType("text/javascript; charset=UTF-8",
                                                                                 PLAIN_TEXT)),
                                                           Map.entry(".json",
                                                                     contentType("application/json; charset=UTF-8", JSON)),
                                                           Map.entry(".png", contentType("image/png", BINARY)),
                                                           Map.entry(".jpg", contentType("image/jpeg", BINARY)),
                                                           Map.entry(".jpeg", contentType("image/jpeg", BINARY)),
                                                           Map.entry(".gif", contentType("image/gif", BINARY)),
                                                           Map.entry(".svg", contentType("image/svg+xml", BINARY)),
                                                           Map.entry(".ico", contentType("image/x-icon", BINARY)),
                                                           Map.entry(".woff", contentType("font/woff", BINARY)),
                                                           Map.entry(".woff2", contentType("font/woff2", BINARY)),
                                                           Map.entry(".ttf", contentType("font/ttf", BINARY)),
                                                           Map.entry(".eot",
                                                                     contentType("application/vnd.ms-fontobject", BINARY)),
                                                           Map.entry(".xml",
                                                                     contentType("application/xml; charset=UTF-8",
                                                                                 PLAIN_TEXT)),
                                                           Map.entry(".txt",
                                                                     contentType("text/plain; charset=UTF-8", PLAIN_TEXT)),
                                                           Map.entry(".map",
                                                                     contentType("application/json; charset=UTF-8", JSON)));

    ContentType DEFAULT_CONTENT_TYPE = CommonContentTypes.APPLICATION_OCTET_STREAM;

    /**
     * Creates a route source for serving static files from the classpath.
     *
     * @param urlPrefix       the URL prefix to match (e.g., "/static")
     * @param classpathPrefix the classpath prefix where files are located (e.g., "/web")
     * @return a route source that serves static files
     */
    static StaticFileRouteSource staticFiles(String urlPrefix, String classpathPrefix) {
        var normalizedUrlPrefix = PathUtils.normalize(urlPrefix);
        var normalizedClasspathPrefix = normalizeClasspathPrefix(classpathPrefix);
        return () -> Stream.of(route(GET,
                                     normalizedUrlPrefix,
                                     createHandler(normalizedUrlPrefix, normalizedClasspathPrefix),
                                     DEFAULT_CONTENT_TYPE));
    }

    private static String normalizeClasspathPrefix(String prefix) {
        var result = prefix.startsWith("/")
                     ? prefix
                     : "/" + prefix;
        return result.endsWith("/")
               ? result.substring(0, result.length() - 1)
               : result;
    }

    private static Handler<byte[]> createHandler(String urlPrefix, String classpathPrefix) {
        return ctx -> {
            var requestPath = ctx.requestPath();
            var relativePath = extractRelativePath(requestPath, urlPrefix);
            return validatePath(relativePath)
            .fold(Cause::promise, validPath -> loadResource(classpathPrefix, validPath, ctx));
        };
    }

    private static String extractRelativePath(String requestPath, String urlPrefix) {
        var path = requestPath.startsWith(urlPrefix)
                   ? requestPath.substring(urlPrefix.length())
                   : requestPath;
        return path.isEmpty()
               ? "/"
               : path;
    }

    private static org.pragmatica.lang.Result<String> validatePath(String path) {
        if (path.contains("..")) {
            return StaticFileError.DIRECTORY_TRAVERSAL.result();
        }
        return org.pragmatica.lang.Result.success(path);
    }

    private static Promise<byte[]> loadResource(String classpathPrefix, String relativePath, RequestContext ctx) {
        var resourcePath = resolveResourcePath(classpathPrefix, relativePath);
        var contentType = detectContentType(resourcePath);
        ctx.responseHeaders()
           .set("Content-Type",
                contentType.headerText());
        ctx.responseHeaders()
           .set("Cache-Control", "no-cache");
        return Promise.lift(StaticFileRouteSource::mapException, () -> readResource(resourcePath));
    }

    private static StaticFileError mapException(Throwable ex) {
        return ex instanceof FileNotFoundException
               ? StaticFileError.NOT_FOUND
               : new StaticFileError.ReadFailed(ex);
    }

    private static String resolveResourcePath(String classpathPrefix, String relativePath) {
        var path = relativePath.endsWith("/")
                   ? relativePath + "index.html"
                   : relativePath;
        return classpathPrefix + (path.startsWith("/")
                                  ? path
                                  : "/" + path);
    }

    private static byte[] readResource(String resourcePath) throws IOException {
        try (InputStream is = StaticFileRouteSource.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            return is.readAllBytes();
        }
    }

    static ContentType detectContentType(String path) {
        var lastDot = path.lastIndexOf('.');
        if (lastDot < 0) {
            return DEFAULT_CONTENT_TYPE;
        }
        var extension = path.substring(lastDot)
                            .toLowerCase();
        return CONTENT_TYPES.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }

    /**
     * Errors that can occur when serving static files.
     */
    sealed interface StaticFileError extends Cause {
        enum General implements StaticFileError {
            DIRECTORY_TRAVERSAL("Directory traversal not allowed"),
            NOT_FOUND("Resource not found");
            private final String message;
            General(String message) {
                this.message = message;
            }
            @Override
            public String message() {
                return message;
            }
        }

        StaticFileError DIRECTORY_TRAVERSAL = General.DIRECTORY_TRAVERSAL;
        StaticFileError NOT_FOUND = General.NOT_FOUND;

        record ReadFailed(Throwable cause) implements StaticFileError {
            @Override
            public String message() {
                return "Failed to read static file: " + cause.getMessage();
            }
        }
    }

    record unused() implements StaticFileRouteSource {
        @Override
        public Stream<Route<?>> routes() {
            return Stream.empty();
        }
    }
}
