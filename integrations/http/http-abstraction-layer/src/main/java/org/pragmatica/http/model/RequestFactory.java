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

package org.pragmatica.http.model;

import org.pragmatica.http.serialization.Deserializer;
import org.pragmatica.http.serialization.Serializer;

/// Factory for creating RequestBuilder instances with pre-assigned request IDs.
/// Used by framework adapters to construct HttpRequest objects.
@FunctionalInterface
public interface RequestFactory {
    /// Create a new RequestBuilder with a generated request ID.
    ///
    /// @return RequestBuilder instance
    RequestBuilder newRequest();

    /// Create a RequestFactory with the specified ID provider and serialization components.
    ///
    /// @param idProvider provider for generating unique request IDs
    /// @param deserializer deserializer for request bodies
    /// @param serializer serializer for response bodies
    /// @return RequestFactory instance
    static RequestFactory requestFactory(
        IdProvider idProvider,
        Deserializer deserializer,
        Serializer serializer
    ) {
        return () -> RequestBuilder.builder(idProvider.generateId(), deserializer, serializer);
    }
}
