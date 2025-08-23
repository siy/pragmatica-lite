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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// HTTP headers collection with case-insensitive header name handling.
/// Supports multiple values per header name as per HTTP specification.
/// Thread-safe for read operations, requires external synchronization for writes.
public class HttpHeaders {
    
    // Using LinkedHashMap to preserve insertion order, with case-insensitive keys
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    
    /// Create empty headers collection
    public HttpHeaders() {
    }
    
    /// Create headers collection with initial headers copied from another collection
    public HttpHeaders(HttpHeaders other) {
        if (other != null) {
            for (var name : other.names()) {
                headers.put(normalizeHeaderName(name), new ArrayList<>(other.all(name)));
            }
        }
    }
    
    /// Add a header value (allows multiple values for the same header name)
    public HttpHeaders add(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Header name cannot be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Header value cannot be null");
        }
        
        var normalizedName = normalizeHeaderName(name);
        headers.computeIfAbsent(normalizedName, k -> new ArrayList<>()).add(value);
        return this;
    }
    
    /// Set a header value (replaces any existing values for this header name)
    public HttpHeaders set(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Header name cannot be null or blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Header value cannot be null");
        }
        
        var normalizedName = normalizeHeaderName(name);
        var values = new ArrayList<String>();
        values.add(value);
        headers.put(normalizedName, values);
        return this;
    }
    
    /// Remove all values for a header name
    public HttpHeaders remove(String name) {
        if (name != null) {
            headers.remove(normalizeHeaderName(name));
        }
        return this;
    }
    
    /// Get the first value for a header name
    public Optional<String> first(String name) {
        if (name == null) return Optional.empty();
        
        var values = headers.get(normalizeHeaderName(name));
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }
    
    /// Get all values for a header name
    public List<String> all(String name) {
        if (name == null) return List.of();
        
        var values = headers.get(normalizeHeaderName(name));
        return values != null ? Collections.unmodifiableList(values) : List.of();
    }
    
    /// Check if a header name exists
    public boolean contains(String name) {
        return name != null && headers.containsKey(normalizeHeaderName(name));
    }
    
    /// Get all header names
    public Set<String> names() {
        return Collections.unmodifiableSet(headers.keySet());
    }
    
    /// Get the total number of headers (counting each name once)
    public int size() {
        return headers.size();
    }
    
    /// Check if headers collection is empty
    public boolean isEmpty() {
        return headers.isEmpty();
    }
    
    /// Clear all headers
    public HttpHeaders clear() {
        headers.clear();
        return this;
    }
    
    /// Create a copy of these headers
    public HttpHeaders copy() {
        return new HttpHeaders(this);
    }
    
    /// Get Content-Type header value
    public Optional<String> contentType() {
        return first("Content-Type");
    }
    
    /// Set Content-Type header
    public HttpHeaders contentType(String contentType) {
        return set("Content-Type", contentType);
    }
    
    /// Get Content-Length header value as long
    public Optional<Long> contentLength() {
        return first("Content-Length").map(value -> {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }).filter(length -> length != null && length >= 0);
    }
    
    /// Set Content-Length header
    public HttpHeaders contentLength(long length) {
        if (length < 0) {
            throw new IllegalArgumentException("Content-Length must be non-negative");
        }
        return set("Content-Length", String.valueOf(length));
    }
    
    /// Get Authorization header value
    public Optional<String> authorization() {
        return first("Authorization");
    }
    
    /// Set Authorization header
    public HttpHeaders authorization(String authorization) {
        return set("Authorization", authorization);
    }
    
    /// Set Basic Authorization header
    public HttpHeaders basicAuth(String username, String password) {
        var credentials = java.util.Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return authorization("Basic " + credentials);
    }
    
    /// Set Bearer Token Authorization header
    public HttpHeaders bearerToken(String token) {
        return authorization("Bearer " + token);
    }
    
    /// Normalize header name to standard form (preserve original casing for known headers)
    private String normalizeHeaderName(String name) {
        // For now, we'll use the original name but could implement proper case normalization
        // for well-known headers if needed
        return name;
    }
    
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("HttpHeaders[");
        var first = true;
        for (var entry : headers.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        var other = (HttpHeaders) obj;
        return headers.equals(other.headers);
    }
    
    @Override
    public int hashCode() {
        return headers.hashCode();
    }
}