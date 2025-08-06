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
import java.util.Objects;

/// URL building and template resolution utility
public interface UrlBuilder {
    
    /// Build URL from base URL, path segments, and query parameters
    String buildUrl(String baseUrl, List<String> pathSegments, Map<String, String> queryParams);
    
    /// Resolve URL template with positional arguments
    String resolveTemplate(String template, Object... args);
    
    /// Resolve URL template with named variables
    String resolveTemplate(String template, Map<String, Object> variables);
    
    /// Create default URL builder implementation
    static UrlBuilder create() {
        record urlBuilder() implements UrlBuilder {
            
            @Override
            public String buildUrl(String baseUrl, List<String> pathSegments, Map<String, String> queryParams) {
                Objects.requireNonNull(baseUrl, "Base URL cannot be null");
                Objects.requireNonNull(pathSegments, "Path segments cannot be null");
                Objects.requireNonNull(queryParams, "Query params cannot be null");
                
                var url = new StringBuilder(baseUrl);
                
                // Add path segments
                for (var segment : pathSegments) {
                    if (segment != null && !segment.isEmpty()) {
                        if (!url.toString().endsWith("/")) {
                            url.append("/");
                        }
                        url.append(URLEncoder.encode(segment, StandardCharsets.UTF_8));
                    }
                }
                
                // Add query parameters
                if (!queryParams.isEmpty()) {
                    url.append("?");
                    var first = true;
                    for (var entry : queryParams.entrySet()) {
                        if (!first) {
                            url.append("&");
                        }
                        url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                           .append("=")
                           .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                        first = false;
                    }
                }
                
                return url.toString();
            }
            
            @Override
            public String resolveTemplate(String template, Object... args) {
                Objects.requireNonNull(template, "Template cannot be null");
                Objects.requireNonNull(args, "Arguments cannot be null");
                
                var result = template;
                var argIndex = 0;
                
                // Replace {} placeholders with positional arguments
                while (result.contains("{}") && argIndex < args.length) {
                    var arg = args[argIndex] != null ? args[argIndex].toString() : "";
                    result = result.replaceFirst("\\{\\}", URLEncoder.encode(arg, StandardCharsets.UTF_8));
                    argIndex++;
                }
                
                return result;
            }
            
            @Override
            public String resolveTemplate(String template, Map<String, Object> variables) {
                Objects.requireNonNull(template, "Template cannot be null");
                Objects.requireNonNull(variables, "Variables cannot be null");
                
                var result = template;
                
                // Replace {name} placeholders with named variables
                for (var entry : variables.entrySet()) {
                    var placeholder = "{" + entry.getKey() + "}";
                    var value = entry.getValue() != null ? entry.getValue().toString() : "";
                    var encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
                    result = result.replace(placeholder, encodedValue);
                }
                
                return result;
            }
        };
        
        return new urlBuilder();
    }
}