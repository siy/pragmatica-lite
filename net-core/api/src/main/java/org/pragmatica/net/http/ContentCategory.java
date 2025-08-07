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

/// Content type categories that indicate how to deserialize the content
public enum ContentCategory {
    /// Plain text content - decode as string
    PLAIN_TEXT,
    
    /// JSON content - deserialize using JSON parser
    JSON,
    
    /// XML content - deserialize using XML parser  
    XML,
    
    /// HTML content - deserialize as string (can be parsed as XML/DOM if needed)
    HTML,
    
    /// Form-encoded data - deserialize using form parser
    FORM_DATA,
    
    /// Binary content - handle as byte array (images, audio, video, documents, etc.)
    BINARY
}