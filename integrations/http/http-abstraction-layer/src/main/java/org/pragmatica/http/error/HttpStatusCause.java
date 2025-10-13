/*
 *  Copyright (c) 2025 Sergiy Yevtushenko.
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
 *
 */

package org.pragmatica.http.error;

import org.pragmatica.http.model.ResponseBuilder;
import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

/// HTTP-aware error cause that can provide HTTP status code and fill response details.
/// Errors implementing this interface are automatically converted to HTTP responses
/// following RFC 7807 Problem Details format.
public interface HttpStatusCause extends Cause {
    /// Get the HTTP status code for this error.
    ///
    /// @return HTTP status code (e.g., 400, 404, 500)
    int httpStatus();

    /// Fill the response builder with error details.
    /// Implementation should set status, content type, and body following RFC 7807 format.
    ///
    /// @param builder response builder to populate
    /// @return Result containing populated builder
    Result<ResponseBuilder> fillResponse(ResponseBuilder builder);
}
