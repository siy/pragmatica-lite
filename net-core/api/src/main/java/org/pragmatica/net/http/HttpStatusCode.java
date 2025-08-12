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

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

public enum HttpStatusCode {
    // 2xx Success
    OK(200, "OK") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.Ok(statusText);
        }
    },
    CREATED(201, "Created") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.Created(statusText);
        }
    },
    ACCEPTED(202, "Accepted") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.Accepted(statusText);
        }
    },
    NO_CONTENT(204, "No Content") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.NoContent(statusText);
        }
    },
    RESET_CONTENT(205, "Reset Content") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.ResetContent(statusText);
        }
    },
    PARTIAL_CONTENT(206, "Partial Content") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Success.PartialContent(statusText);
        }
    },
    
    // 3xx Redirection
    MULTIPLE_CHOICES(300, "Multiple Choices") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.MultipleChoices(statusText);
        }
    },
    MOVED_PERMANENTLY(301, "Moved Permanently") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.MovedPermanently(statusText);
        }
    },
    FOUND(302, "Found") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.Found(statusText);
        }
    },
    SEE_OTHER(303, "See Other") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.SeeOther(statusText);
        }
    },
    NOT_MODIFIED(304, "Not Modified") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.NotModified(statusText);
        }
    },
    TEMPORARY_REDIRECT(307, "Temporary Redirect") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.TemporaryRedirect(statusText);
        }
    },
    PERMANENT_REDIRECT(308, "Permanent Redirect") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.Redirection.PermanentRedirect(statusText);
        }
    },
    
    // 4xx Client Error
    BAD_REQUEST(400, "Bad Request") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.BadRequest(statusText);
        }
    },
    UNAUTHORIZED(401, "Unauthorized") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.Unauthorized(statusText);
        }
    },
    PAYMENT_REQUIRED(402, "Payment Required") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.PaymentRequired(statusText);
        }
    },
    FORBIDDEN(403, "Forbidden") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.Forbidden(statusText);
        }
    },
    NOT_FOUND(404, "Not Found") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.NotFound(statusText);
        }
    },
    METHOD_NOT_ALLOWED(405, "Method Not Allowed") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.MethodNotAllowed(statusText);
        }
    },
    NOT_ACCEPTABLE(406, "Not Acceptable") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.NotAcceptable(statusText);
        }
    },
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.ProxyAuthenticationRequired(statusText);
        }
    },
    REQUEST_TIMEOUT(408, "Request Timeout") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.RequestTimeout(statusText);
        }
    },
    CONFLICT(409, "Conflict") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.Conflict(statusText);
        }
    },
    GONE(410, "Gone") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.Gone(statusText);
        }
    },
    LENGTH_REQUIRED(411, "Length Required") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.LengthRequired(statusText);
        }
    },
    PRECONDITION_FAILED(412, "Precondition Failed") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.PreconditionFailed(statusText);
        }
    },
    PAYLOAD_TOO_LARGE(413, "Payload Too Large") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.PayloadTooLarge(statusText);
        }
    },
    URI_TOO_LONG(414, "URI Too Long") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.UriTooLong(statusText);
        }
    },
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.UnsupportedMediaType(statusText);
        }
    },
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.RangeNotSatisfiable(statusText);
        }
    },
    EXPECTATION_FAILED(417, "Expectation Failed") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.ExpectationFailed(statusText);
        }
    },
    IM_A_TEAPOT(418, "I'm a teapot") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.ImATeapot(statusText);
        }
    },
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.UnprocessableEntity(statusText);
        }
    },
    TOO_MANY_REQUESTS(429, "Too Many Requests") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ClientError.TooManyRequests(statusText);
        }
    },
    
    // 5xx Server Error
    INTERNAL_SERVER_ERROR(500, "Internal Server Error") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.InternalServerError(statusText);
        }
    },
    NOT_IMPLEMENTED(501, "Not Implemented") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.NotImplemented(statusText);
        }
    },
    BAD_GATEWAY(502, "Bad Gateway") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.BadGateway(statusText);
        }
    },
    SERVICE_UNAVAILABLE(503, "Service Unavailable") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.ServiceUnavailable(statusText);
        }
    },
    GATEWAY_TIMEOUT(504, "Gateway Timeout") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.GatewayTimeout(statusText);
        }
    },
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.HttpVersionNotSupported(statusText);
        }
    },
    INSUFFICIENT_STORAGE(507, "Insufficient Storage") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.InsufficientStorage(statusText);
        }
    },
    LOOP_DETECTED(508, "Loop Detected") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.LoopDetected(statusText);
        }
    },
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required") {
        @Override
        public HttpError asError(String statusText) {
            return new HttpError.ServerError.NetworkAuthenticationRequired(statusText);
        }
    };
    
    private final int code;
    private final String defaultMessage;
    
    private static final Map<Integer, HttpStatusCode> CODE_MAP = 
        Arrays.stream(values())
              .collect(Collectors.toMap(HttpStatusCode::code, status -> status));
    
    HttpStatusCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
    
    public int code() {
        return code;
    }
    
    public String defaultMessage() {
        return defaultMessage;
    }
    
    /// Abstract method that each enum constant implements to create specific HttpError
    public abstract HttpError asError(String statusText);
    
    /// Returns Result<HttpStatusCode> for given integer status code
    public static Result<HttpStatusCode> fromCode(int statusCode) {
        return Option.option(CODE_MAP.get(statusCode))
                    .toResult(HttpError.UnknownStatusCode.create(statusCode));
    }
    
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }
    
    public boolean isRedirection() {
        return code >= 300 && code < 400;
    }
    
    public boolean isClientError() {
        return code >= 400 && code < 500;
    }
    
    public boolean isServerError() {
        return code >= 500 && code < 600;
    }
}