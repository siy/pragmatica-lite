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

import org.pragmatica.lang.io.TimeSpan;

/// Configuration for HTTP client instances.
/// Provides settings for timeouts, connection pooling, and other HTTP client behavior.
/// Immutable configuration object.
public record HttpClientConfig(
    TimeSpan connectTimeout,
    TimeSpan requestTimeout,  
    TimeSpan readTimeout,
    int maxConnections,
    int maxConnectionsPerHost,
    boolean followRedirects,
    String userAgent,
    HttpHeaders defaultHeaders
) {
    
    public HttpClientConfig {
        if (connectTimeout == null) {
            throw new IllegalArgumentException("Connect timeout cannot be null");
        }
        if (requestTimeout == null) {
            throw new IllegalArgumentException("Request timeout cannot be null");
        }
        if (readTimeout == null) {
            throw new IllegalArgumentException("Read timeout cannot be null");
        }
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max connections must be positive");
        }
        if (maxConnectionsPerHost <= 0) {
            throw new IllegalArgumentException("Max connections per host must be positive");
        }
        if (defaultHeaders == null) {
            throw new IllegalArgumentException("Default headers cannot be null");
        }
    }
    
    /// Create default HTTP client configuration
    public static HttpClientConfig defaults() {
        return new HttpClientConfig(
            TimeSpan.seconds(5),     // 5 second connect timeout
            TimeSpan.seconds(30),    // 30 second request timeout
            TimeSpan.seconds(30),    // 30 second read timeout
            200,                     // max 200 total connections
            20,                      // max 20 connections per host
            true,                    // follow redirects
            "Pragmatica-Http-Client/1.0", // default user agent
            new HttpHeaders()        // empty default headers
        );
    }
    
    /// Create a builder for customizing configuration
    public static Builder builder() {
        return new Builder();
    }
    
    /// Create a builder starting from this configuration
    public Builder toBuilder() {
        return new Builder()
            .connectTimeout(connectTimeout)
            .requestTimeout(requestTimeout)
            .readTimeout(readTimeout)
            .maxConnections(maxConnections)
            .maxConnectionsPerHost(maxConnectionsPerHost)
            .followRedirects(followRedirects)
            .userAgent(userAgent)
            .defaultHeaders(new HttpHeaders(defaultHeaders));
    }
    
    /// Builder for HTTP client configuration
    public static class Builder {
        private TimeSpan connectTimeout = TimeSpan.seconds(5);
        private TimeSpan requestTimeout = TimeSpan.seconds(30);
        private TimeSpan readTimeout = TimeSpan.seconds(30);
        private int maxConnections = 200;
        private int maxConnectionsPerHost = 20;
        private boolean followRedirects = true;
        private String userAgent = "Pragmatica-Http-Client/1.0";
        private HttpHeaders defaultHeaders = new HttpHeaders();
        
        public Builder connectTimeout(TimeSpan timeout) {
            this.connectTimeout = timeout;
            return this;
        }
        
        public Builder requestTimeout(TimeSpan timeout) {
            this.requestTimeout = timeout;
            return this;
        }
        
        public Builder readTimeout(TimeSpan timeout) {
            this.readTimeout = timeout;
            return this;
        }
        
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        public Builder maxConnectionsPerHost(int maxConnectionsPerHost) {
            this.maxConnectionsPerHost = maxConnectionsPerHost;
            return this;
        }
        
        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder defaultHeaders(HttpHeaders headers) {
            this.defaultHeaders = headers != null ? new HttpHeaders(headers) : new HttpHeaders();
            return this;
        }
        
        public Builder addDefaultHeader(String name, String value) {
            if (this.defaultHeaders == null) {
                this.defaultHeaders = new HttpHeaders();
            }
            this.defaultHeaders.add(name, value);
            return this;
        }
        
        public HttpClientConfig build() {
            return new HttpClientConfig(
                connectTimeout,
                requestTimeout,
                readTimeout,
                maxConnections,
                maxConnectionsPerHost,
                followRedirects,
                userAgent,
                defaultHeaders
            );
        }
    }
}