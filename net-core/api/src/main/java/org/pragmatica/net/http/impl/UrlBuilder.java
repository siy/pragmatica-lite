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

/// URL building utility
public interface UrlBuilder {
    
    /// Build URL from base URL, path segments, and query parameters
    String buildUrl(String baseUrl, List<String> pathSegments, Map<String, String> queryParams);
    
    /// Create default URL builder implementation
    static UrlBuilder create() {
        record urlBuilder() implements UrlBuilder {
            
            @Override
            public String buildUrl(String baseUrl, List<String> pathSegments, Map<String, String> queryParams) {
                
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
        };
        
        return new urlBuilder();
    }
}