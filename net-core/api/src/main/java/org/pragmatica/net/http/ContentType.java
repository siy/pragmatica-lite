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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashMap;

/// Represents an HTTP Content-Type with media type, subtype, and parameters.
/// Immutable and thread-safe.
public record ContentType(String type, String subtype, Map<String, String> parameters) {
    
    public ContentType {
        Objects.requireNonNull(type, "Content type cannot be null");
        Objects.requireNonNull(subtype, "Content subtype cannot be null");
        Objects.requireNonNull(parameters, "Content type parameters cannot be null");
        
        // Ensure parameters map is immutable
        parameters = Map.copyOf(parameters);
        
        if (type.isBlank()) {
            throw new IllegalArgumentException("Content type cannot be blank");
        }
        if (subtype.isBlank()) {
            throw new IllegalArgumentException("Content subtype cannot be blank");
        }
    }
    
    /// Get the full media type (type/subtype)
    public String mediaType() {
        return type + "/" + subtype;
    }
    
    /// Get the charset parameter if present
    public Optional<Charset> charset() {
        return Optional.ofNullable(parameters.get("charset"))
                      .map(charsetName -> {
                          try {
                              return Charset.forName(charsetName);
                          } catch (Exception e) {
                              return null;
                          }
                      });
    }
    
    /// Create a new ContentType with the specified charset
    public ContentType withCharset(Charset charset) {
        var newParams = new LinkedHashMap<>(parameters);
        newParams.put("charset", charset.name());
        return new ContentType(type, subtype, newParams);
    }
    
    /// Create a new ContentType with the specified parameter
    public ContentType withParameter(String name, String value) {
        Objects.requireNonNull(name, "Parameter name cannot be null");
        Objects.requireNonNull(value, "Parameter value cannot be null");
        
        var newParams = new LinkedHashMap<>(parameters);
        newParams.put(name.toLowerCase(), value);
        return new ContentType(type, subtype, newParams);
    }
    
    /// Check if this content type matches another (ignores parameters)
    public boolean matches(ContentType other) {
        if (other == null) return false;
        return type.equalsIgnoreCase(other.type) && subtype.equalsIgnoreCase(other.subtype);
    }
    
    /// Check if this content type is compatible with another (allows wildcards)
    public boolean isCompatibleWith(ContentType other) {
        if (other == null) return false;
        
        var typeMatches = "*".equals(type) || "*".equals(other.type) || 
                         type.equalsIgnoreCase(other.type);
        var subtypeMatches = "*".equals(subtype) || "*".equals(other.subtype) || 
                            subtype.equalsIgnoreCase(other.subtype);
        
        return typeMatches && subtypeMatches;
    }
    
    /// Parse a ContentType from a string representation
    public static ContentType parse(String contentTypeString) {
        if (contentTypeString == null || contentTypeString.isBlank()) {
            throw new IllegalArgumentException("Content type string cannot be null or blank");
        }
        
        // Split by semicolon to separate media type from parameters
        var parts = contentTypeString.split(";");
        var mediaTypePart = parts[0].trim();
        
        // Split media type into type and subtype
        var mediaTypeParts = mediaTypePart.split("/", 2);
        if (mediaTypeParts.length != 2) {
            throw new IllegalArgumentException("Invalid media type: " + mediaTypePart);
        }
        
        var type = mediaTypeParts[0].trim().toLowerCase();
        var subtype = mediaTypeParts[1].trim().toLowerCase();
        
        var parameters = new LinkedHashMap<String, String>();
        
        // Parse parameters
        for (int i = 1; i < parts.length; i++) {
            var paramPart = parts[i].trim();
            var paramParts = paramPart.split("=", 2);
            
            if (paramParts.length == 2) {
                var paramName = paramParts[0].trim().toLowerCase();
                var paramValue = paramParts[1].trim();
                
                // Remove quotes if present
                if (paramValue.startsWith("\"") && paramValue.endsWith("\"") && paramValue.length() > 1) {
                    paramValue = paramValue.substring(1, paramValue.length() - 1);
                }
                
                parameters.put(paramName, paramValue);
            }
        }
        
        return new ContentType(type, subtype, parameters);
    }
    
    /// Create a ContentType with just media type (no parameters)
    public static ContentType of(String type, String subtype) {
        return new ContentType(type, subtype, Map.of());
    }
    
    /// Create a ContentType from media type string
    public static ContentType ofMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            throw new IllegalArgumentException("Media type cannot be null or blank");
        }
        
        var parts = mediaType.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid media type: " + mediaType);
        }
        
        return new ContentType(parts[0].trim().toLowerCase(), parts[1].trim().toLowerCase(), Map.of());
    }
    
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(type).append("/").append(subtype);
        
        for (var entry : parameters.entrySet()) {
            sb.append("; ").append(entry.getKey()).append("=");
            var value = entry.getValue();
            // Add quotes if value contains spaces or special characters
            if (value.contains(" ") || value.contains(";") || value.contains(",")) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }
        
        return sb.toString();
    }
}