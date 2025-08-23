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

package org.pragmatica.net.http;

/// HTTP status codes as defined in RFC 7231 and common extensions.
/// Represents both the numeric status code and associated reason phrase.
public record HttpStatus(int code, String reasonPhrase) {
    
    // === 1xx Informational ===
    public static final HttpStatus CONTINUE = new HttpStatus(100, "Continue");
    public static final HttpStatus SWITCHING_PROTOCOLS = new HttpStatus(101, "Switching Protocols");
    public static final HttpStatus PROCESSING = new HttpStatus(102, "Processing");
    
    // === 2xx Success ===
    public static final HttpStatus OK = new HttpStatus(200, "OK");
    public static final HttpStatus CREATED = new HttpStatus(201, "Created");
    public static final HttpStatus ACCEPTED = new HttpStatus(202, "Accepted");
    public static final HttpStatus NON_AUTHORITATIVE_INFORMATION = new HttpStatus(203, "Non-Authoritative Information");
    public static final HttpStatus NO_CONTENT = new HttpStatus(204, "No Content");
    public static final HttpStatus RESET_CONTENT = new HttpStatus(205, "Reset Content");
    public static final HttpStatus PARTIAL_CONTENT = new HttpStatus(206, "Partial Content");
    
    // === 3xx Redirection ===
    public static final HttpStatus MULTIPLE_CHOICES = new HttpStatus(300, "Multiple Choices");
    public static final HttpStatus MOVED_PERMANENTLY = new HttpStatus(301, "Moved Permanently");
    public static final HttpStatus FOUND = new HttpStatus(302, "Found");
    public static final HttpStatus SEE_OTHER = new HttpStatus(303, "See Other");
    public static final HttpStatus NOT_MODIFIED = new HttpStatus(304, "Not Modified");
    public static final HttpStatus USE_PROXY = new HttpStatus(305, "Use Proxy");
    public static final HttpStatus TEMPORARY_REDIRECT = new HttpStatus(307, "Temporary Redirect");
    public static final HttpStatus PERMANENT_REDIRECT = new HttpStatus(308, "Permanent Redirect");
    
    // === 4xx Client Error ===
    public static final HttpStatus BAD_REQUEST = new HttpStatus(400, "Bad Request");
    public static final HttpStatus UNAUTHORIZED = new HttpStatus(401, "Unauthorized");
    public static final HttpStatus PAYMENT_REQUIRED = new HttpStatus(402, "Payment Required");
    public static final HttpStatus FORBIDDEN = new HttpStatus(403, "Forbidden");
    public static final HttpStatus NOT_FOUND = new HttpStatus(404, "Not Found");
    public static final HttpStatus METHOD_NOT_ALLOWED = new HttpStatus(405, "Method Not Allowed");
    public static final HttpStatus NOT_ACCEPTABLE = new HttpStatus(406, "Not Acceptable");
    public static final HttpStatus PROXY_AUTHENTICATION_REQUIRED = new HttpStatus(407, "Proxy Authentication Required");
    public static final HttpStatus REQUEST_TIMEOUT = new HttpStatus(408, "Request Timeout");
    public static final HttpStatus CONFLICT = new HttpStatus(409, "Conflict");
    public static final HttpStatus GONE = new HttpStatus(410, "Gone");
    public static final HttpStatus LENGTH_REQUIRED = new HttpStatus(411, "Length Required");
    public static final HttpStatus PRECONDITION_FAILED = new HttpStatus(412, "Precondition Failed");
    public static final HttpStatus PAYLOAD_TOO_LARGE = new HttpStatus(413, "Payload Too Large");
    public static final HttpStatus URI_TOO_LONG = new HttpStatus(414, "URI Too Long");
    public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = new HttpStatus(415, "Unsupported Media Type");
    public static final HttpStatus RANGE_NOT_SATISFIABLE = new HttpStatus(416, "Range Not Satisfiable");
    public static final HttpStatus EXPECTATION_FAILED = new HttpStatus(417, "Expectation Failed");
    public static final HttpStatus TOO_MANY_REQUESTS = new HttpStatus(429, "Too Many Requests");
    
    // === 5xx Server Error ===
    public static final HttpStatus INTERNAL_SERVER_ERROR = new HttpStatus(500, "Internal Server Error");
    public static final HttpStatus NOT_IMPLEMENTED = new HttpStatus(501, "Not Implemented");
    public static final HttpStatus BAD_GATEWAY = new HttpStatus(502, "Bad Gateway");
    public static final HttpStatus SERVICE_UNAVAILABLE = new HttpStatus(503, "Service Unavailable");
    public static final HttpStatus GATEWAY_TIMEOUT = new HttpStatus(504, "Gateway Timeout");
    public static final HttpStatus HTTP_VERSION_NOT_SUPPORTED = new HttpStatus(505, "HTTP Version Not Supported");
    
    /// Check if this status indicates informational response (1xx)
    public boolean isInformational() {
        return code >= 100 && code < 200;
    }
    
    /// Check if this status indicates success (2xx)
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }
    
    /// Check if this status indicates redirection (3xx)
    public boolean isRedirection() {
        return code >= 300 && code < 400;
    }
    
    /// Check if this status indicates client error (4xx)
    public boolean isClientError() {
        return code >= 400 && code < 500;
    }
    
    /// Check if this status indicates server error (5xx)
    public boolean isServerError() {
        return code >= 500 && code < 600;
    }
    
    /// Check if this status indicates any kind of error (4xx or 5xx)
    public boolean isError() {
        return isClientError() || isServerError();
    }
    
    /// Create a status from code, using standard reason phrase if known
    public static HttpStatus fromCode(int code) {
        return fromCode(code, getStandardReasonPhrase(code));
    }
    
    /// Create a status from code and custom reason phrase
    public static HttpStatus fromCode(int code, String reasonPhrase) {
        return new HttpStatus(code, reasonPhrase != null ? reasonPhrase : "Unknown");
    }
    
    /// Get the standard reason phrase for a status code, or "Unknown" if not recognized
    private static String getStandardReasonPhrase(int code) {
        return switch (code) {
            // 1xx
            case 100 -> "Continue";
            case 101 -> "Switching Protocols";
            case 102 -> "Processing";
            
            // 2xx
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 203 -> "Non-Authoritative Information";
            case 204 -> "No Content";
            case 205 -> "Reset Content";
            case 206 -> "Partial Content";
            
            // 3xx
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 304 -> "Not Modified";
            case 305 -> "Use Proxy";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
            
            // 4xx
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 402 -> "Payment Required";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 406 -> "Not Acceptable";
            case 407 -> "Proxy Authentication Required";
            case 408 -> "Request Timeout";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 411 -> "Length Required";
            case 412 -> "Precondition Failed";
            case 413 -> "Payload Too Large";
            case 414 -> "URI Too Long";
            case 415 -> "Unsupported Media Type";
            case 416 -> "Range Not Satisfiable";
            case 417 -> "Expectation Failed";
            case 429 -> "Too Many Requests";
            
            // 5xx
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 505 -> "HTTP Version Not Supported";
            
            default -> "Unknown";
        };
    }
    
    @Override
    public String toString() {
        return code + " " + reasonPhrase;
    }
}