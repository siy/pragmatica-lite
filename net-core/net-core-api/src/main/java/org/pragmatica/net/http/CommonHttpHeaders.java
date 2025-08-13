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

/// Common HTTP header names as enum constants
public enum CommonHttpHeaders {
    ACCEPT("accept"),
    ACCEPT_ENCODING("accept-encoding"), 
    AUTHORIZATION("authorization"),
    CONTENT_TYPE("content-type"),
    CONTENT_LENGTH("content-length"),
    USER_AGENT("user-agent"),
    HOST("host"),
    CONNECTION("connection");
    
    private final String headerName;
    
    CommonHttpHeaders(String headerName) {
        this.headerName = headerName;
    }
    
    /// Returns the lowercase header name
    public String headerName() {
        return headerName;
    }
    
    @Override
    public String toString() {
        return headerName;
    }
}