package org.pragmatica.http.routing;

import org.pragmatica.lang.Option;

import java.util.regex.Pattern;

/**
 * Utility methods for HTTP path normalization.
 */
public sealed interface PathUtils {
    Pattern MULTISLASH = Pattern.compile("/+");

    /**
     * Normalize a URL path by:
     * - Stripping query parameters
     * - Collapsing multiple slashes
     * - Ensuring path starts and ends with /
     */
    static String normalize(String fullPath) {
        return Option.option(fullPath)
                     .map(PathUtils::normalizeNonNull)
                     .or("/");
    }

    private static String normalizeNonNull(String fullPath) {
        var query = fullPath.indexOf('?');
        var path = query >= 0
                   ? fullPath.substring(0, query)
                             .strip()
                   : fullPath.strip();
        if (path.isBlank() || "/".equals(path)) {
            return "/";
        }
        var stringPath = MULTISLASH.matcher("/" + path)
                                   .replaceAll("/");
        var index = stringPath.lastIndexOf('/');
        if (index < (stringPath.length() - 1)) {
            return stringPath + "/";
        } else {
            return stringPath;
        }
    }

    record unused() implements PathUtils {}
}
