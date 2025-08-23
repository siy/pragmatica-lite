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

/// Interface for serializing objects to byte arrays.
/// Implementations handle specific serialization formats (JSON, binary, etc.)
public interface Serializer {
    
    /// Serialize an object to byte array
    byte[] serialize(Object object);
    
    /// Check if this serializer can handle the given object type
    boolean canSerialize(Class<?> type);
    
    /// Get the content type produced by this serializer
    String contentType();
}