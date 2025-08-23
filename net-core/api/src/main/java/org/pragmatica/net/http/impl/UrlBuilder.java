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

package org.pragmatica.net.http.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/// Utility class for building URLs from components like base URL, path segments, and query parameters.
/// Handles proper URL encoding and formatting.
public class UrlBuilder {
    
    /// Create a new UrlBuilder instance
    public static UrlBuilder create() {
        return new UrlBuilder();
    }
    
    /// Build a complete URL from components
    public String buildUrl(String baseUrl, List<String> pathSegments, Map<String, String> queryParams) {
        var sb = new StringBuilder();
        
        // Add base URL
        if (baseUrl != null && !baseUrl.isBlank()) {
            sb.append(baseUrl);
            
            // Ensure base URL doesn't end with slash for consistent path building
            if (sb.charAt(sb.length() - 1) == '/') {
                sb.setLength(sb.length() - 1);
            }
        }
        
        // Add path segments
        if (pathSegments != null && !pathSegments.isEmpty()) {
            for (var segment : pathSegments) {
                if (segment != null && !segment.isBlank()) {
                    sb.append('/').append(urlEncode(segment));
                }
            }
        }
        
        // Add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append('?');
            var first = true;
            for (var entry : queryParams.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(urlEncode(entry.getKey()));
                if (entry.getValue() != null) {
                    sb.append('=').append(urlEncode(entry.getValue()));
                }
            }
        }
        
        return sb.toString();
    }
    
    /// Resolve a template string with provided arguments
    /// Template format: /users/{}/posts/{} where {} are placeholders
    public String resolveTemplate(String template, Object... args) {
        if (template == null) {
            return "";
        }
        
        var result = template;
        var argIndex = 0;
        
        while (result.contains("{}") && argIndex < args.length) {
            var placeholder = "{}";
            var replacement = args[argIndex] != null ? urlEncode(args[argIndex].toString()) : "";
            result = result.replaceFirst("\\{\\}", replacement);
            argIndex++;
        }
        
        return result;
    }
    
    /// URL-encode a string using UTF-8
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}