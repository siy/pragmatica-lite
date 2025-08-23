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

package org.pragmatica.net.serialization;

import org.pragmatica.lang.type.TypeToken;

/// Interface for deserializing objects from byte arrays.
/// Implementations handle specific serialization formats (JSON, binary, etc.)
public interface Deserializer {
    
    /// Deserialize byte array to object of specified type
    <T> T deserialize(byte[] data, Class<T> type);
    
    /// Deserialize byte array to object using TypeToken for generic type safety
    <T> T deserialize(byte[] data, TypeToken<T> type);
    
    /// Check if this deserializer can handle the given content type
    boolean canDeserialize(String contentType);
    
    /// Get the content types this deserializer can handle
    String[] supportedContentTypes();
}