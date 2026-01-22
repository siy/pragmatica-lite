/*
 *  Copyright (c) 2020-2025 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.pragmatica.http.server;

import org.pragmatica.http.CommonContentType;
import org.pragmatica.http.ContentType;
import org.pragmatica.http.HttpStatus;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Option;

import java.nio.charset.StandardCharsets;

/// Response writer for HTTP handlers.
///
/// Each response method can only be called once per request.
public interface ResponseWriter {
    /// Standard header name for request ID tracing.
    String X_REQUEST_ID = "X-Request-Id";

    /// Write response with status, body, and content type.
    void write(HttpStatus status, byte[] body, ContentType contentType);

    /// Add header to the response. Must be called before write methods.
    ResponseWriter header(String name, String value);

    // Convenience methods
    /// Write successful JSON response.
    default void ok(String json) {
        write(HttpStatus.OK, json.getBytes(StandardCharsets.UTF_8), CommonContentType.APPLICATION_JSON);
    }

    /// Write successful response with content type.
    default void ok(byte[] body, ContentType contentType) {
        write(HttpStatus.OK, body, contentType);
    }

    /// Write successful text response.
    default void okText(String text) {
        write(HttpStatus.OK, text.getBytes(StandardCharsets.UTF_8), CommonContentType.TEXT_PLAIN);
    }

    /// Write successful HTML response.
    default void okHtml(String html) {
        write(HttpStatus.OK, html.getBytes(StandardCharsets.UTF_8), CommonContentType.TEXT_HTML);
    }

    /// Write 204 No Content response.
    default void noContent() {
        write(HttpStatus.NO_CONTENT, new byte[0], CommonContentType.TEXT_PLAIN);
    }

    /// Write 404 Not Found response.
    default void notFound() {
        error(HttpStatus.NOT_FOUND, "Not Found");
    }

    /// Write 400 Bad Request response.
    default void badRequest(String message) {
        error(HttpStatus.BAD_REQUEST, message);
    }

    /// Write error response.
    default void error(HttpStatus status, String message) {
        var json = "{\"error\":\"" + escapeJson(message) + "\"}";
        write(status, json.getBytes(StandardCharsets.UTF_8), CommonContentType.APPLICATION_JSON);
    }

    /// Write 500 Internal Server Error response.
    default void internalError(Cause cause) {
        error(HttpStatus.INTERNAL_SERVER_ERROR, cause.message());
    }

    /// Write JSON response with status.
    default void respond(HttpStatus status, String json) {
        write(status, json.getBytes(StandardCharsets.UTF_8), CommonContentType.APPLICATION_JSON);
    }

    private static String escapeJson(String s) {
        return Option.option(s)
                     .map(ResponseWriter::doEscapeJson)
                     .or("");
    }

    private static String doEscapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
